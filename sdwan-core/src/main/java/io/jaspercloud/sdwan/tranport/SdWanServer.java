package io.jaspercloud.sdwan.tranport;

import cn.hutool.core.collection.CollUtil;
import com.google.protobuf.AbstractMessageLite;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.support.ChannelAttributes;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class SdWanServer implements Lifecycle, Runnable {

    private SdWanServerConfig config;
    private Supplier<ChannelHandler> handler;

    private Channel localChannel;
    private Cidr ipPool;
    private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();
    private Map<String, String> fixedVipMap = new ConcurrentHashMap<>();
    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    private Map<Channel, String> registChannelMap = new ConcurrentHashMap<>();

    public SdWanServer(SdWanServerConfig config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    private void processMsg(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
        switch (msg.getType().getNumber()) {
            case SDWanProtos.MessageTypeCode.HeartType_VALUE: {
                log.debug("update heart: {}", SocketAddressUtil.toAddress(ctx.channel().remoteAddress()));
                ChannelAttributes.attr(ctx.channel()).setLastHeartTime(System.currentTimeMillis());
                SdWanServer.reply(ctx.channel(), msg, SDWanProtos.MessageTypeCode.HeartType, null);
                break;
            }
            case SDWanProtos.MessageTypeCode.RegistReqType_VALUE: {
                processRegist(ctx, msg);
                break;
            }
            case SDWanProtos.MessageTypeCode.P2pOfferType_VALUE: {
                processP2pOffer(ctx, msg);
                break;
            }
            case SDWanProtos.MessageTypeCode.P2pAnswerType_VALUE: {
                processP2pAnswer(ctx, msg);
                break;
            }
        }
    }

    private void processP2pAnswer(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
        SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.parseFrom(msg.getData());
        Channel channel = bindIPMap.get(p2pAnswer.getDstVIP()).get();
        if (null != channel) {
            SdWanServer.push(channel, msg.getReqId(), SDWanProtos.MessageTypeCode.P2pAnswerType, p2pAnswer);
        }
    }

    private void processP2pOffer(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
        SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(msg.getData());
        Channel channel = bindIPMap.get(p2pOffer.getDstVIP()).get();
        if (null != channel) {
            SdWanServer.push(channel, msg.getReqId(), SDWanProtos.MessageTypeCode.P2pOfferType, p2pOffer);
        }
    }

    private void processRegist(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        Channel channel = ctx.channel();
        //regist
        try {
            SDWanProtos.RegistReq registReq = SDWanProtos.RegistReq.parseFrom(msg.getData());
            ChannelAttributes attr = ChannelAttributes.attr(channel);
            String vip = registChannelMap.computeIfAbsent(channel, key -> {
                String retVip = applyVip(channel, registReq, attr);
                channel.closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        registChannelMap.remove(channel);
                    }
                });
                return retVip;
            });
            attr.setMacAddress(registReq.getMacAddress());
            attr.setAddressUriList(registReq.getAddressUriList());
            SDWanProtos.NodeInfoList nodeInfoList = SDWanProtos.NodeInfoList.newBuilder()
                    .addAllNodeInfo(registChannelMap.keySet().stream()
                            .map(ch -> {
                                ChannelAttributes chAttr = ChannelAttributes.attr(ch);
                                return SDWanProtos.NodeInfo.newBuilder()
                                        .setVip(chAttr.getVip())
                                        .addAllAddressUri(chAttr.getAddressUriList())
                                        .build();
                            }).collect(Collectors.toList()))
                    .build();
            SDWanProtos.RouteList.Builder routeBuilder = SDWanProtos.RouteList.newBuilder();
            if (!CollUtil.isEmpty(config.getRouteList())) {
                config.getRouteList().forEach(e -> {
                    routeBuilder.addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination(e.getDestination())
                            .addAllNexthop(e.getNexthop())
                            .build());
                });
            }
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.Success)
                    .setVip(vip)
                    .setMaskBits(ipPool.getMaskBits())
                    .setNodeList(nodeInfoList)
                    .setRouteList(routeBuilder.build())
                    .build();
            SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.RegistRespType, regResp);
            sendAllChannelNodeOnline(channel);
            channel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    sendAllChannelNodeOffline(channel);
                }
            });
        } catch (ProcessCodeException e) {
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.forNumber(e.getCode()))
                    .build();
            SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.RegistRespType, regResp);
        } catch (Exception e) {
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError)
                    .build();
            SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.RegistRespType, regResp);
        }
    }

    private void sendAllChannelNodeOnline(Channel channel) {
        ChannelAttributes attr = ChannelAttributes.attr(channel);
        SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                .setVip(attr.getVip())
                .addAllAddressUri(attr.getAddressUriList())
                .build();
        for (Channel item : registChannelMap.keySet()) {
            if (item.id().asShortText().equals(channel.id().asShortText())) {
                continue;
            }
            try {
                SdWanServer.push(item, SDWanProtos.MessageTypeCode.NodeOnlineType, nodeInfo);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void sendAllChannelNodeOffline(Channel channel) {
        ChannelAttributes attr = ChannelAttributes.attr(channel);
        SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                .setVip(attr.getVip())
                .addAllAddressUri(attr.getAddressUriList())
                .build();
        for (Channel item : registChannelMap.keySet()) {
            try {
                SdWanServer.push(item, SDWanProtos.MessageTypeCode.NodeOfflineType, nodeInfo);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private String applyVip(Channel channel, SDWanProtos.RegistReq registReq, ChannelAttributes attr) {
        String fixedIp = fixedVipMap.get(registReq.getMacAddress());
        if (null != fixedIp) {
            if (!bindIPMap.get(fixedIp).compareAndSet(null, channel)) {
                throw new ProcessCodeException(SDWanProtos.MessageCode.VipBound_VALUE);
            }
            bindVip(channel, attr, fixedIp);
            return fixedIp;
        }
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
            if (entry.getValue().compareAndSet(null, channel)) {
                String vip = entry.getKey();
                bindVip(channel, attr, vip);
                return vip;
            }
        }
        throw new ProcessCodeException(SDWanProtos.MessageCode.NotEnough_VALUE);
    }

    private void bindVip(Channel channel, ChannelAttributes attr, String vip) {
        attr.setVip(vip);
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bindIPMap.get(vip).set(null);
            }
        });
    }

    public void push(SDWanProtos.MessageTypeCode msgCode, AbstractMessageLite data) {
        for (Channel channel : registChannelMap.keySet()) {
            push(channel, msgCode, data);
        }
    }

    public static void push(Channel channel, SDWanProtos.MessageTypeCode msgCode, AbstractMessageLite data) {
        push(channel, UUID.randomUUID().toString(), msgCode, data);
    }

    public static void push(Channel channel, String id, SDWanProtos.MessageTypeCode msgCode, AbstractMessageLite data) {
        SDWanProtos.Message req = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.Push)
                .setType(msgCode)
                .setData(data.toByteString())
                .build();
        channel.writeAndFlush(req);
    }

    public static void reply(Channel channel, SDWanProtos.Message msg, SDWanProtos.MessageTypeCode msgCode, AbstractMessageLite data) {
        SDWanProtos.Message.Builder builder = msg.toBuilder().setType(msgCode);
        if (null != data) {
            builder.setData(data.toByteString());
        }
        channel.writeAndFlush(builder.build());
    }

    @Override
    public void start() throws Exception {
        if (!CollUtil.isEmpty(config.getFixedVipList())) {
            config.getFixedVipList().forEach(e -> {
                fixedVipMap.put(e.getMac(), e.getVip());
            });
        }
        ipPool = Cidr.parseCidr(config.getVipCidr());
        ipPool.getAvailableIpList()
                .forEach(vip -> {
                    bindIPMap.put(vip, new AtomicReference<>());
                });
        NioEventLoopGroup bossGroup = NioEventLoopFactory.createBossGroup();
        NioEventLoopGroup workerGroup = NioEventLoopFactory.createWorkerGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new TcpLogHandler("sdwanServer"));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(SDWanProtos.Message.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<SDWanProtos.Message>() {

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                Channel channel = ctx.channel();
                                ChannelAttributes attr = ChannelAttributes.attr(channel);
                                attr.setLastHeartTime(System.currentTimeMillis());
                                channel.closeFuture().addListener(new ChannelFutureListener() {
                                    @Override
                                    public void operationComplete(ChannelFuture future) throws Exception {
                                        channelMap.remove(channel.id().asShortText());
                                    }
                                });
                                channelMap.put(channel.id().asShortText(), channel);
                                super.channelActive(ctx);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                ctx.channel().close();
                                super.exceptionCaught(ctx, cause);
                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                                processMsg(ctx, msg);
                            }
                        });
                        pipeline.addLast("sdwanServer:process", handler.get());
                    }
                });
        try {
            localChannel = serverBootstrap.bind(config.getPort()).syncUninterruptibly().channel();
            log.info("sdwan server started: port={}", config.getPort());
            bossGroup.scheduleAtFixedRate(this, 0, 2 * config.getHeartTimeout(), TimeUnit.MILLISECONDS);
            localChannel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }

    @Override
    public void run() {
        Iterator<Map.Entry<String, Channel>> iterator = channelMap.entrySet().iterator();
        while (iterator.hasNext()) {
            try {
                Map.Entry<String, Channel> next = iterator.next();
                Channel channel = next.getValue();
                ChannelAttributes attrs = ChannelAttributes.attr(channel);
                long diff = System.currentTimeMillis() - attrs.getLastHeartTime();
                if (diff > config.getHeartTimeout()) {
                    log.info("timeout: chId={}, vip={}", channel.id().asShortText(), attrs.getVip());
                    channel.close();
                    iterator.remove();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
