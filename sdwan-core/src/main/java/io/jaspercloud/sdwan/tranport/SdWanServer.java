package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.AbstractMessageLite;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.ChannelAttributes;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class SdWanServer implements InitializingBean, DisposableBean, Runnable {

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

    private void processRegist(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
        //regist
        {
            Channel channel = ctx.channel();
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
            SDWanProtos.RouteList.Builder routeBuilder = SDWanProtos.RouteList.newBuilder();
            if (!CollectionUtils.isEmpty(config.getRouteList())) {
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
                    .setRouteList(routeBuilder.build())
                    .build();
            SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.RegistRespType, regResp);
        }
        //nodeInfoList
        {
            SDWanProtos.NodeInfoList.Builder builder = SDWanProtos.NodeInfoList.newBuilder();
            for (Channel channel : registChannelMap.keySet()) {
                ChannelAttributes attr = ChannelAttributes.attr(channel);
                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                        .setVip(attr.getVip())
                        .addAllAddressUri(attr.getAddressUriList())
                        .build();
                builder.addNodeInfo(nodeInfo);
            }
            SDWanProtos.NodeInfoList nodeInfoList = builder.build();
            for (Channel channel : registChannelMap.keySet()) {
                try {
                    SdWanServer.push(channel, SDWanProtos.MessageTypeCode.NodeInfoListType, nodeInfoList);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private String applyVip(Channel channel, SDWanProtos.RegistReq registReq, ChannelAttributes attr) {
        String fixedIp = fixedVipMap.get(registReq.getMacAddress());
        if (null != fixedIp) {
            if (!bindIPMap.get(fixedIp).compareAndSet(null, channel)) {
                throw new ProcessException();
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
        throw new ProcessException();
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
    public void afterPropertiesSet() throws Exception {
        if (!CollectionUtils.isEmpty(config.getFixedVipMap())) {
            fixedVipMap.putAll(config.getFixedVipMap());
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
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
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
    }

    @Override
    public void destroy() throws Exception {
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
