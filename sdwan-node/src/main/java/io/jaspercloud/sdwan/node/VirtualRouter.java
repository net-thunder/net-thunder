package io.jaspercloud.sdwan.node;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.support.Multicast;
import io.jaspercloud.sdwan.tranport.SdWanClient;
import io.jaspercloud.sdwan.tranport.SdWanClientConfig;
import io.jaspercloud.sdwan.tranport.TransportLifecycle;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.tranport.rule.RouteRulePredicateChain;
import io.jaspercloud.sdwan.tranport.rule.RouteRulePredicateProcessor;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.windows.Ics;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.util.PlatformUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class VirtualRouter implements TransportLifecycle {

    public static final String NodeVersion = "1.0.2";

    private SdWanNodeConfig config;
    private volatile boolean showRouteRuleLog = false;

    private SdWanClient sdWanClient;
    private NodeManager nodeManager;
    private IceClient iceClient;
    private String localVip;
    private int maskBits;
    private String vipCidr;
    private volatile List<SDWanProtos.Route> routeList = Collections.emptyList();
    private volatile RouteRulePredicateProcessor routeInRuleProcessor = new RouteRulePredicateProcessor();
    private volatile RouteRulePredicateProcessor routeOutRuleProcessor = new RouteRulePredicateProcessor();
    private volatile Map<String, SDWanProtos.VNAT> vnatMap = Collections.emptyMap();
    private Cache<String, String> vnatMappingCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    private AtomicBoolean status = new AtomicBoolean(false);

    public String getLocalVip() {
        return localVip;
    }

    public int getMaskBits() {
        return maskBits;
    }

    public String getVipCidr() {
        return vipCidr;
    }

    public List<SDWanProtos.Route> getRouteList() {
        return routeList;
    }

    public SdWanClient getSdWanClient() {
        return sdWanClient;
    }

    public IceClient getIceClient() {
        return iceClient;
    }

    public VirtualRouter(SdWanNodeConfig config) {
        this.config = config;
    }

    public void send(IpLayerPacket packet) {
        if (config.getNetMesh()
                && PlatformDependent.isWindows()
                && Ics.IcsIp.equals(packet.getSrcIP())) {
            //fix ics
            packet.setSrcIP(localVip);
        }
        if (Multicast.isMulticastIp(packet.getDstIP()) || Cidr.isBroadcastAddress(vipCidr, packet.getDstIP())) {
            //broadcast
            byte[] bytes = SDWanProtos.IpPacket.newBuilder()
                    .setSrcIP(packet.getSrcIP())
                    .setDstIP(packet.getDstIP())
                    .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(packet.rebuild())))
                    .build().toByteArray();
            nodeManager.getNodeList().forEach(nodeInfo -> {
                if (StringUtils.equals(nodeInfo.getVip(), localVip)) {
                    return;
                }
                iceClient.sendNode(localVip, nodeInfo, bytes);
            });
            return;
        }
        //route
        String dstVip = routeOut(packet);
        if (null == dstVip) {
            return;
        }
        SDWanProtos.NodeInfo nodeInfo = nodeManager.getNode(dstVip);
        if (null == nodeInfo) {
            return;
        }
        ByteBuf byteBuf = packet.rebuild();
        byte[] bytes = SDWanProtos.IpPacket.newBuilder()
                .setSrcIP(packet.getSrcIP())
                .setDstIP(packet.getDstIP())
                .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(byteBuf)))
                .build().toByteArray();
        iceClient.sendNode(localVip, nodeInfo, bytes);
    }

    public IpLayerPacket routeIn(IpLayerPacket packet) {
        if (!routeInRuleProcessor.test(packet.getDstIP())) {
            if (showRouteRuleLog) {
                log.info("reject routeIn: {}", packet);
            }
            return null;
        }
        processVnatIn(packet);
        return packet;
    }

    public String routeOut(IpLayerPacket packet) {
        processVnatOut(packet);
        if (!routeOutRuleProcessor.test(packet.getDstIP())) {
            if (showRouteRuleLog) {
                log.info("reject routeOut: {}", packet);
            }
            return null;
        }
        String dstIP = packet.getDstIP();
        if (Cidr.contains(vipCidr, dstIP)) {
            return dstIP;
        }
        dstIP = findRoute(packet);
        return dstIP;
    }

    private void processVnatIn(IpLayerPacket packet) {
        SDWanProtos.VNAT vnat = findVNAT(vnatMap, packet, IpLayerPacket::getDstIP);
        if (null != vnat) {
            String dstIP = packet.getDstIP();
            String natIp = natIp(vnat, dstIP);
            packet.setDstIP(natIp);
            String identifier = packet.getIdentifier(true);
            vnatMappingCache.put(identifier, dstIP);
        }
    }

    private void processVnatOut(IpLayerPacket packet) {
        String originalIp = vnatMappingCache.getIfPresent(packet.getIdentifier());
        if (null != originalIp) {
            packet.setSrcIP(originalIp);
        }
    }

    private String natIp(SDWanProtos.VNAT vnat, String ip) {
        Cidr from = Cidr.parseCidr(vnat.getSrc());
        Cidr to = Cidr.parseCidr(vnat.getDst());
        String natIp = Cidr.transform(ip, from, to);
        return natIp;
    }

    private SDWanProtos.VNAT findVNAT(Map<String, SDWanProtos.VNAT> map, IpLayerPacket packet, Function<IpLayerPacket, String> in) {
        for (Map.Entry<String, SDWanProtos.VNAT> entry : map.entrySet()) {
            String ip = in.apply(packet);
            String cidr = entry.getKey();
            if (Cidr.contains(cidr, ip)) {
                SDWanProtos.VNAT vnat = entry.getValue();
                return vnat;
            }
        }
        return null;
    }

    private String findRoute(IpLayerPacket packet) {
        String dstIP = packet.getDstIP();
        for (SDWanProtos.Route route : routeList) {
            if (Cidr.contains(route.getDestination(), dstIP)) {
                ProtocolStringList nexthopList = route.getNexthopList();
                if (nexthopList.isEmpty()) {
                    return null;
                }
                int rand = Math.abs(packet.getSrcIP().hashCode()) % nexthopList.size();
                String nexthop = nexthopList.get(rand);
                return nexthop;
            }
        }
        return null;
    }

    @Override
    public void start() throws Exception {
        nodeManager = new NodeManager();
        sdWanClient = new SdWanClient(SdWanClientConfig.builder()
                .controllerServer(config.getControllerServer())
                .tenantId(config.getTenantId())
                .connectTimeout(config.getConnectTimeout())
                .heartTime(config.getHeartTime())
                .build(),
                () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        onSdWanClientInactive();
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                        switch (msg.getType().getNumber()) {
                            case SDWanProtos.MessageTypeCode.P2pOfferType_VALUE: {
                                SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(msg.getData());
                                if (iceClient.isRunning()) {
                                    iceClient.processOffer(msg.getReqId(), p2pOffer);
                                }
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.P2pAnswerType_VALUE: {
                                AsyncTask.completeTask(msg.getReqId(), msg);
                                break;
                            }
//                            case SDWanProtos.MessageTypeCode.RouteListType_VALUE: {
//                                SDWanProtos.RouteList routeList = SDWanProtos.RouteList.parseFrom(msg.getData());
//                                break;
//                            }
//                            case SDWanProtos.MessageTypeCode.RouteRuleListType_VALUE: {
//                                SDWanProtos.RouteRuleList routeRuleList = SDWanProtos.RouteRuleList.parseFrom(msg.getData());
//                                break;
//                            }
//                            case SDWanProtos.MessageTypeCode.VNATListType_VALUE: {
//                                SDWanProtos.VNATList vnatList = SDWanProtos.VNATList.parseFrom(msg.getData());
//                                break;
//                            }
                            case SDWanProtos.MessageTypeCode.NodeOnlineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                log.info("onlineNode: vip={}", nodeInfo.getVip());
                                nodeManager.addNode(nodeInfo.getVip(), nodeInfo);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeOfflineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                log.info("offlineNode: vip={}", nodeInfo.getVip());
                                nodeManager.delNode(nodeInfo.getVip());
                                if (iceClient.isRunning()) {
                                    iceClient.offlineTransport(nodeInfo.getVip());
                                }
                                break;
                            }
                        }
                    }
                });
        iceClient = new IceClient(config, sdWanClient, () -> new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new SimpleChannelInboundHandler<StunPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, StunPacket msg) throws Exception {
                        InetSocketAddress sender = msg.sender();
                        StunMessage stunMessage = msg.content();
                        StringAttr transferTypeAttr = stunMessage.getAttr(AttrType.TransferType);
                        BytesAttr dataAttr = stunMessage.getAttr(AttrType.Data);
                        byte[] data = dataAttr.getData();
                        if (!MessageType.Transfer.equals(stunMessage.getMessageType())) {
                            return;
                        }
                        SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.parseFrom(data);
                        if (config.getShowVRouterLog()) {
                            log.info("recvICE: type={}, sender={}, src={}, dst={}",
                                    transferTypeAttr.getData(), SocketAddressUtil.toAddress(sender),
                                    ipPacket.getSrcIP(), ipPacket.getDstIP());
                        }
                        ByteBuf byteBuf = ByteBufUtil.toByteBuf(ipPacket.getPayload().toByteArray());
                        try {
                            IpLayerPacket packet = new IpLayerPacket(byteBuf);
                            packet = routeIn(packet);
                            onData(VirtualRouter.this, packet);
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
            }
        });
        nodeManager.start();
        sdWanClient.start();
        //getConfig
        SDWanProtos.ServerConfigResp configResp = sdWanClient.getConfig(config.getConnectTimeout()).get();
        if (!SDWanProtos.MessageCode.Success.equals(configResp.getCode())) {
            throw new ProcessException("get config error");
        }
        applyLocalAddress();
        config.setStunServerList(configResp.getStunServersList());
        config.setRelayServerList(configResp.getRelayServersList());
        String macAddress = processMacAddress(NetworkInterfaceUtil.getHardwareAddress(config.getLocalAddress()));
        log.info("parseMacAddress: {}", macAddress);
        SDWanProtos.RegistReq.Builder builder = SDWanProtos.RegistReq.newBuilder()
                .setTenantId(config.getTenantId())
                .setNodeType(SDWanProtos.NodeTypeCode.SimpleType)
                .setMacAddress(macAddress)
                .addAllAddressUri(Collections.emptyList())
                .setOs(PlatformUtil.normalizedOs())
                .setOsVersion(System.getProperty("os.name"))
                .setNodeVersion(NodeVersion);
        SDWanProtos.RegistReq registReq = builder.build();
        //1.先regist
        SDWanProtos.RegistResp regResp = sdWanClient.regist(registReq, 3000).get();
        if (!SDWanProtos.MessageCode.Success.equals(regResp.getCode())) {
            throw new ProcessCodeException(regResp.getCode().getNumber(), "registSdwan failed=" + regResp.getCode().name());
        }
        log.info("applyLocalVip: vip={}", regResp.getVip());
        localVip = regResp.getVip();
        maskBits = regResp.getMaskBits();
        vipCidr = regResp.getCidr();
        regResp.getNodeList().getNodeInfoList().forEach(e -> {
            nodeManager.addNode(e.getVip(), e);
        });
        routeList = buildMergeRouteList(regResp.getRouteList().getRouteList(), regResp.getVnatList().getVnatList());
        onUpdateRouteList(routeList);
        List<RouteRulePredicateChain> routeRuleList = buildRouteRuleList(regResp.getRouteRuleList().getRouteRuleList());
        List<RouteRulePredicateChain> routeInRuleList = routeRuleList.stream()
                .filter(e -> Arrays.asList(RouteRuleDirectionEnum.All, RouteRuleDirectionEnum.Input).contains(e.getDirection()))
                .collect(Collectors.toList());
        List<RouteRulePredicateChain> routeOutRuleList = routeRuleList.stream()
                .filter(e -> Arrays.asList(RouteRuleDirectionEnum.All, RouteRuleDirectionEnum.Output).contains(e.getDirection()))
                .collect(Collectors.toList());
        routeInRuleProcessor = new RouteRulePredicateProcessor(routeInRuleList);
        routeOutRuleProcessor = new RouteRulePredicateProcessor(routeOutRuleList);
        vnatMap = buildVNATMap(regResp.getVnatList().getVnatList());
        //2.再start iceClient，不然updateNodeInfo上报不了
        iceClient.applyLocalVip(localVip);
        iceClient.start();
        status.set(true);
    }

    protected void onData(VirtualRouter router, IpLayerPacket packet) {

    }

    protected void onUpdateRouteList(List<SDWanProtos.Route> routeList) {

    }

    protected void onSdWanClientInactive() {

    }

    private List<SDWanProtos.Route> buildMergeRouteList(List<SDWanProtos.Route> routeList, List<SDWanProtos.VNAT> vnatList) {
        List<SDWanProtos.Route> list = new ArrayList<>();
        list.addAll(routeList);
        List<SDWanProtos.Route> collect = vnatList.stream().map(e -> {
            SDWanProtos.Route route = SDWanProtos.Route.newBuilder()
                    .setDestination(e.getSrc())
                    .addAllNexthop(e.getVipListList())
                    .build();
            return route;
        }).collect(Collectors.toList());
        list.addAll(collect);
        list.forEach(e -> {
            log.info("addRoute: destination={}, nexthop={}",
                    e.getDestination(), StringUtils.join(e.getNexthopList(), ","));
        });
        return list;
    }

    private List<RouteRulePredicateChain> buildRouteRuleList(List<SDWanProtos.RouteRule> routeRuleList) {
        routeRuleList.forEach(e -> {
            log.info("addRouteRule: strategy={}, direction={}, ruleList={}",
                    e.getStrategy(), e.getDirection(), StringUtils.join(e.getRuleListList(), ","));
        });
        List<RouteRulePredicateChain> collect = routeRuleList.stream()
                .sorted(RouteRulePredicateChain.comparator())
                .map(e -> RouteRulePredicateChain.build(e))
                .collect(Collectors.toList());
        return collect;
    }

    private Map<String, SDWanProtos.VNAT> buildVNATMap(List<SDWanProtos.VNAT> vnatList) {
        Map<String, SDWanProtos.VNAT> map = new ConcurrentHashMap();
        vnatList.forEach(e -> {
            log.info("addVNAT: src={}, dst={}, vipList={}",
                    e.getSrc(), e.getDst(), StringUtils.join(e.getVipListList(), ","));
            map.put(e.getSrc(), e);
        });
        return map;
    }

    private void applyLocalAddress() {
        String localAddress = config.getLocalAddress();
        if (null == localAddress) {
            localAddress = sdWanClient.getLocalAddress();
            log.info("parseLocalAddress: {}", localAddress);
        }
        if (null == localAddress) {
            localAddress = config.getHostAddress();
        }
        config.setLocalAddress(localAddress);
    }

    protected String processMacAddress(String hardwareAddress) {
        return hardwareAddress;
    }

    @Override
    public void stop() throws Exception {
        if (null != nodeManager) {
            nodeManager.stop();
            nodeManager = null;
        }
        if (null != iceClient) {
            iceClient.stop();
            iceClient = null;
        }
        if (null != sdWanClient) {
            sdWanClient.stop();
            sdWanClient = null;
        }
        status.set(false);
    }

    @Override
    public boolean isRunning() {
        if (null == sdWanClient) {
            return false;
        }
        return sdWanClient.isRunning();
    }
}
