package io.jaspercloud.sdwan.tranport;

import cn.hutool.core.collection.CollectionUtil;
import com.google.protobuf.AbstractMessageLite;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.ChannelAttributes;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.config.*;
import io.jaspercloud.sdwan.tranport.service.SdWanDataService;
import io.jaspercloud.sdwan.util.ShortUUID;
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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class SdWanServer implements Lifecycle, Runnable {

    private SdWanServerConfig config;
    private SdWanDataService sdWanDataService;
    private Supplier<ChannelHandler> handler;

    private Channel localChannel;
    private Map<String, Channel> channelMap = new ConcurrentHashMap<>();
    private Map<Channel, NodeConfig> registChannelMap = new ConcurrentHashMap<>();
    private Map<String, ChannelSpace> channelSpaceMap = new ConcurrentHashMap<>();

    public Channel getChannelSpace(String tenantId, String vip) {
        ChannelSpace space = channelSpaceMap.get(tenantId);
        if (null == space) {
            return null;
        }
        if (null == vip) {
            return null;
        }
        Channel channel = space.getChannel(vip);
        return channel;
    }

    public int getOnlineChannel(String tenantId) {
        ChannelSpace space = channelSpaceMap.get(tenantId);
        if (null == space) {
            return 0;
        }
        int count = space.count();
        return count;
    }

    public SdWanServer(SdWanServerConfig config, SdWanDataService sdWanDataService, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.sdWanDataService = sdWanDataService;
        this.handler = handler;
    }

    public void offlineChannel(String tenantCode, String vip) {
        ChannelSpace channelSpace = channelSpaceMap.get(tenantCode);
        if (null == channelSpace) {
            return;
        }
        Channel channel = channelSpace.getChannel(vip);
        if (null == channel) {
            return;
        }
        channel.close();
    }

    public void offlineChannelList(String tenantCode) {
        ChannelSpace channelSpace = channelSpaceMap.get(tenantCode);
        if (null == channelSpace) {
            return;
        }
        for (Channel channel : channelSpace.getChannelList()) {
            channel.close();
        }
    }

    private void processMsg(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        switch (msg.getType().getNumber()) {
            case SDWanProtos.MessageTypeCode.HeartType_VALUE: {
                Channel channel = ctx.channel();
                if (log.isDebugEnabled()) {
                    log.debug("update heart: {}", SocketAddressUtil.toAddress(channel.remoteAddress()));
                }
                ChannelAttributes.attr(channel).setLastHeartTime(System.currentTimeMillis());
                SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.HeartType, null);
                break;
            }
            case SDWanProtos.MessageTypeCode.UpdateNodeInfoType_VALUE: {
                processUpdateNodeInfo(ctx, msg);
                break;
            }
            case SDWanProtos.MessageTypeCode.ConfigReqType_VALUE: {
                processConfig(ctx, msg);
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

    private void processUpdateNodeInfo(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        try {
            Channel channel = ctx.channel();
            ChannelAttributes attr = ChannelAttributes.attr(channel);
            SDWanProtos.NodeInfoReq req = SDWanProtos.NodeInfoReq.parseFrom(msg.getData());
            List<String> addressList = req.getAddressUriList().stream().collect(Collectors.toList());
            attr.setAddressUriList(addressList);
            updateNodeInfoToAllChannel(channel);
        } catch (Exception e) {
            SDWanProtos.ServerConfigResp resp = SDWanProtos.ServerConfigResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError)
                    .setStunServer("")
                    .setRelayServer("")
                    .addAllStunServers(Collections.emptyList())
                    .addAllRelayServers(Collections.emptyList())
                    .build();
            SdWanServer.reply(ctx.channel(), msg, SDWanProtos.MessageTypeCode.ConfigRespTpe, resp);
        }
    }

    private void processConfig(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        try {
            SDWanProtos.ServerConfigReq req = SDWanProtos.ServerConfigReq.parseFrom(msg.getData());
            TenantConfig tenantConfig = sdWanDataService.getTenantConfig(req.getTenantId());
            List<String> stunServerList = tenantConfig.getStunServerList();
            List<String> relayServerList = tenantConfig.getRelayServerList();
            SDWanProtos.ServerConfigResp resp = SDWanProtos.ServerConfigResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.Success)
                    .setStunServer(stunServerList.get(0))
                    .setRelayServer(relayServerList.get(0))
                    .addAllStunServers(stunServerList)
                    .addAllRelayServers(relayServerList)
                    .build();
            SdWanServer.reply(ctx.channel(), msg, SDWanProtos.MessageTypeCode.ConfigRespTpe, resp);
        } catch (Exception e) {
            SDWanProtos.ServerConfigResp resp = SDWanProtos.ServerConfigResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError)
                    .build();
            SdWanServer.reply(ctx.channel(), msg, SDWanProtos.MessageTypeCode.ConfigRespTpe, resp);
        }
    }

    private void processP2pOffer(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        try {
            SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(msg.getData());
            ChannelSpace channelSpace = getChannelSpace(p2pOffer.getTenantId());
            Channel channel = channelSpace.getChannel(p2pOffer.getDstVIP());
            if (null != channel) {
                log.info("push processP2pOffer: srcVIP={}, dstVIP={}, id={}", p2pOffer.getSrcVIP(), p2pOffer.getDstVIP(), msg.getReqId());
                SdWanServer.push(channel, msg.getReqId(), SDWanProtos.MessageTypeCode.P2pOfferType, p2pOffer);
            }
        } catch (Exception e) {
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError)
                    .build();
            SdWanServer.reply(ctx.channel(), msg, SDWanProtos.MessageTypeCode.P2pOfferType, regResp);
        }
    }

    private void processP2pAnswer(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        try {
            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.parseFrom(msg.getData());
            ChannelSpace channelSpace = getChannelSpace(p2pAnswer.getTenantId());
            Channel channel = channelSpace.getChannel(p2pAnswer.getDstVIP());
            if (null != channel) {
                log.info("push processP2pAnswer: srcVIP={}, dstVIP={}, id={}", p2pAnswer.getSrcVIP(), p2pAnswer.getDstVIP(), msg.getReqId());
                SdWanServer.push(channel, msg.getReqId(), SDWanProtos.MessageTypeCode.P2pAnswerType, p2pAnswer);
            }
        } catch (Exception e) {
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError)
                    .build();
            SdWanServer.reply(ctx.channel(), msg, SDWanProtos.MessageTypeCode.P2pAnswerType, regResp);
        }
    }

    private void processRegist(ChannelHandlerContext ctx, SDWanProtos.Message msg) {
        Channel channel = ctx.channel();
        //regist
        try {
            SDWanProtos.RegistReq registReq = SDWanProtos.RegistReq.parseFrom(msg.getData());
            ChannelSpace channelSpace = getChannelSpace(registReq.getTenantId());
            TenantConfig tenantConfig = sdWanDataService.getTenantConfig(registReq.getTenantId());
            ChannelAttributes attr = ChannelAttributes.attr(channel);
            NodeConfig thisNodeConfig = registChannelMap.computeIfAbsent(channel, key -> {
                channel.closeFuture().addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        registChannelMap.remove(channel);
                    }
                });
                NodeConfig nodeConfig = sdWanDataService.assignNodeInfo(channel, registReq.getTenantId(), registReq.getMacAddress());
                attr.setVip(nodeConfig.getVip());
                channelSpace.addChannel(nodeConfig.getVip(), channel);
                return nodeConfig;
            });
            attr.setTenantId(registReq.getTenantId());
            attr.setMacAddress(registReq.getMacAddress());
            attr.setOs(registReq.getOs());
            attr.setOsVersion(registReq.getOsVersion());
            attr.setAddressUriList(registReq.getAddressUriList());
            attr.setNodeVersion(registReq.getNodeVersion());
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
            Cidr ipPool = tenantConfig.getIpPool();
            String vip = thisNodeConfig.getVip();
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.Success)
                    .setVip(vip)
                    .setMaskBits(ipPool.getMaskBits())
                    .setCidr(String.format("%s/%s", ipPool.getNetworkIdentifier(), ipPool.getMaskBits()))
                    .setNodeList(nodeInfoList)
                    .setRouteList(SDWanProtos.RouteList.newBuilder()
                            .addAllRoute(buildRouteList(thisNodeConfig.getRouteConfigList()))
                            .build())
                    .setVnatList(SDWanProtos.VNATList.newBuilder()
                            .addAllVnat(buildVNATList(thisNodeConfig.getVnatConfigList()))
                            .build())
                    .setRouteRuleList(SDWanProtos.RouteRuleList.newBuilder()
                            .addAllRouteRule(buildRouteRuleList(thisNodeConfig.getRouteRuleConfigList()))
                            .build())
                    .build();
            SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.RegistRespType, regResp);
            updateNodeInfoToAllChannel(channel);
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
            log.error(e.getMessage(), e);
            SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.newBuilder()
                    .setCode(SDWanProtos.MessageCode.SysError)
                    .build();
            SdWanServer.reply(channel, msg, SDWanProtos.MessageTypeCode.RegistRespType, regResp);
        }
    }

    private List<SDWanProtos.Route> buildRouteList(List<RouteConfig> list) {
        List<SDWanProtos.Route> collect = list.stream().map(e -> {
            return SDWanProtos.Route.newBuilder()
                    .setDestination(e.getDestination())
                    .addAllNexthop(e.getNexthop())
                    .build();
        }).collect(Collectors.toList());
        return collect;
    }

    private List<SDWanProtos.VNAT> buildVNATList(List<VNATConfig> list) {
        List<SDWanProtos.VNAT> collect = list.stream().map(e -> {
            List<String> vipList = e.getVipList();
            String vip;
            if (CollectionUtil.isNotEmpty(vipList)) {
                vip = vipList.get(0);
            } else {
                vip = "";
            }
            return SDWanProtos.VNAT.newBuilder()
                    .setVip(vip)
                    .setSrc(e.getSrcCidr())
                    .setDst(e.getDstCidr())
                    .addAllVipList(vipList)
                    .build();
        }).collect(Collectors.toList());
        return collect;
    }

    private List<SDWanProtos.RouteRule> buildRouteRuleList(List<RouteRuleConfig> list) {
        list.sort((o1, o2) -> Integer.compare(o1.getLevel(), o2.getLevel()));
        List<SDWanProtos.RouteRule> collect = list.stream()
                .map(e -> {
                    return SDWanProtos.RouteRule.newBuilder()
                            .setStrategy(e.getStrategy().name())
                            .setDirection(e.getDirection().name())
                            .setLevel(e.getLevel())
                            .addAllRuleList(e.getRuleList())
                            .build();
                }).collect(Collectors.toList());
        return collect;
    }

    private void updateNodeInfoToAllChannel(Channel channel) {
        ChannelAttributes attr = ChannelAttributes.attr(channel);
        SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                .setVip(attr.getVip())
                .addAllAddressUri(attr.getAddressUriList())
                .build();
        for (Channel item : registChannelMap.keySet()) {
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

    public void push(SDWanProtos.MessageTypeCode msgCode, AbstractMessageLite data) {
        for (Channel channel : registChannelMap.keySet()) {
            push(channel, msgCode, data);
        }
    }

    public static void push(Channel channel, SDWanProtos.MessageTypeCode msgCode, AbstractMessageLite data) {
        push(channel, ShortUUID.gen(), msgCode, data);
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

    private ChannelSpace getChannelSpace(String tenantId) {
        ChannelSpace channelSpace = channelSpaceMap.computeIfAbsent(tenantId, k -> {
            boolean hasTenant = sdWanDataService.hasTenant(tenantId);
            if (!hasTenant) {
                throw new ProcessException("not found tenant");
            }
            ChannelSpace space = new ChannelSpace();
            return space;
        });
        return channelSpace;
    }

    @Override
    public void start() throws Exception {
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
