package io.jaspercloud.sdwan.node;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.node.rule.CidrRouteRulePredicate;
import io.jaspercloud.sdwan.route.VirtualRouter;
import io.jaspercloud.sdwan.route.rule.RouteRulePredicate;
import io.jaspercloud.sdwan.stun.NatAddress;
import io.jaspercloud.sdwan.support.AddressUri;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.support.Multicast;
import io.jaspercloud.sdwan.tranport.Lifecycle;
import io.jaspercloud.sdwan.tranport.SdWanClient;
import io.jaspercloud.sdwan.tranport.SdWanClientConfig;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.windows.Ics;
import io.jaspercloud.sdwan.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class BaseSdWanNode implements Lifecycle, Runnable {

    private SdWanNodeConfig config;

    private SdWanClient sdWanClient;
    private IceClient iceClient;
    private String localVip;
    private int maskBits;
    private String vipCidr;
    private VirtualRouter virtualRouter;
    private AtomicBoolean status = new AtomicBoolean(false);
    private AtomicBoolean loopStatus = new AtomicBoolean(false);
    private Thread loopThread;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private List<String> localAddressUriList = new ArrayList<>();
    private List<EventListener> eventListenerList = new ArrayList<>();
    private Map<String, SDWanProtos.NodeInfo> nodeInfoMap = new ConcurrentHashMap<>();

    public boolean getStatus() {
        return status.get();
    }

    public List<String> getLocalAddressUriList() {
        return localAddressUriList;
    }

    public String getLocalVip() {
        return localVip;
    }

    public int getMaskBits() {
        return maskBits;
    }

    public String getVipCidr() {
        return vipCidr;
    }

    public VirtualRouter getVirtualRouter() {
        return virtualRouter;
    }

    public SdWanClient getSdWanClient() {
        return sdWanClient;
    }

    public IceClient getIceClient() {
        return iceClient;
    }

    public void addEventListener(EventListener listener) {
        eventListenerList.add(listener);
    }

    public BaseSdWanNode(SdWanNodeConfig config) {
        this.config = config;
    }

    static {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
    }

    @Override
    public synchronized void start() throws Exception {
        CheckAdmin.check();
        sdWanClient = new SdWanClient(SdWanClientConfig.builder()
                .controllerServer(config.getControllerServer())
                .tenantId(config.getTenantId())
                .connectTimeout(config.getConnectTimeout())
                .heartTime(config.getHeartTime())
                .build(),
                () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        log.info("sdWanClient channelInactive");
                        if (getStatus() && !config.getAutoReconnect()) {
                            fireEvent(EventListener::onErrorDisconnected);
                        }
                        signalAll();
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                        switch (msg.getType().getNumber()) {
                            case SDWanProtos.MessageTypeCode.P2pOfferType_VALUE: {
                                SDWanProtos.P2pOffer p2pOffer = SDWanProtos.P2pOffer.parseFrom(msg.getData());
                                iceClient.processOffer(msg.getReqId(), p2pOffer);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.P2pAnswerType_VALUE: {
                                AsyncTask.completeTask(msg.getReqId(), msg);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.RouteListType_VALUE: {
                                SDWanProtos.RouteList routeList = SDWanProtos.RouteList.parseFrom(msg.getData());
                                virtualRouter.updateRoutes(routeList.getRouteList());
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.RouteRuleListType_VALUE: {
                                SDWanProtos.RouteRuleList routeRuleList = SDWanProtos.RouteRuleList.parseFrom(msg.getData());
                                virtualRouter.updateRouteRules(buildRouteRuleList(routeRuleList.getRouteRuleList()));
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.VNATListType_VALUE: {
                                SDWanProtos.VNATList vnatList = SDWanProtos.VNATList.parseFrom(msg.getData());
                                virtualRouter.updateVNATs(vnatList.getVnatList());
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeOnlineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                nodeInfoMap.put(nodeInfo.getVip(), nodeInfo);
                                if (log.isDebugEnabled()) {
                                    log.debug("onlineNode: vip={}", nodeInfo.getVip());
                                }
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeOfflineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                nodeInfoMap.remove(nodeInfo.getVip());
                                if (log.isDebugEnabled()) {
                                    log.debug("offlineNode: vip={}", nodeInfo.getVip());
                                }
                                break;
                            }
                        }
                    }
                });
        iceClient = new IceClient(config, this, () -> new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(getProcessHandler());
            }
        });
        virtualRouter = new VirtualRouter();
        install();
        log.info("SdWanNode started: vip={}", getLocalVip());
        if (config.getAutoReconnect()) {
            loopStatus.set(true);
        }
        loopThread = new Thread(this, "loop");
        loopThread.start();
        status.set(true);
    }

    public void registNodeInfo(List<AddressUri> addressUriList) throws Exception {
        List<AddressUri> list = new ArrayList<>();
        if (config.isOnlyRelayTransport()) {
            List<AddressUri> collect = addressUriList.stream()
                    .filter(e -> AddressType.RELAY.equals(e.getScheme()))
                    .collect(Collectors.toList());
            list.addAll(collect);
        } else {
            List<NetworkInterfaceInfo> interfaceInfoList;
            if (null == config.getLocalAddress()) {
                InetAddress[] inetAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
                interfaceInfoList = NetworkInterfaceUtil.parseInetAddress(inetAddresses);
            } else {
                NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findIp(config.getLocalAddress());
                interfaceInfoList = Arrays.asList(networkInterfaceInfo);
            }
            interfaceInfoList.forEach(e -> {
                String address = e.getInterfaceAddress().getAddress().getHostAddress();
                AddressUri addressUri = AddressUri.builder()
                        .scheme(AddressType.HOST)
                        .host(address)
                        .port(iceClient.getP2pClient().getLocalPort())
                        .build();
                list.add(addressUri);
            });
            list.addAll(addressUriList);
        }
        localAddressUriList = list.stream().map(e -> e.toString()).collect(Collectors.toList());
        sdWanClient.updateNodeInfo(localAddressUriList);
    }

    @Override
    public synchronized void stop() throws Exception {
        if (!status.get()) {
            return;
        }
        log.info("SdWanNode stopping");
        status.set(false);
        loopStatus.set(false);
        loopThread.interrupt();
        uninstall();
        log.info("SdWanNode stopped");
    }

    public void sendIpLayerPacket(IpLayerPacket packet) {
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
            nodeInfoMap.values().forEach(nodeInfo -> {
                if (StringUtils.equals(nodeInfo.getVip(), localVip)) {
                    return;
                }
                iceClient.sendNode(localVip, nodeInfo, bytes);
            });
            return;
        }
        //route
        String dstVip = virtualRouter.routeOut(packet);
        if (null == dstVip) {
            return;
        }
        SDWanProtos.NodeInfo nodeInfo = nodeInfoMap.get(dstVip);
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

    public void sendIpPacket(SDWanProtos.IpPacket ipPacket) {
        Ipv4Packet.Packet packet = Ipv4Packet.builder()
                .version((short) 4)
                .protocol(Ipv4Packet.Icmp)
                .srcIP(ipPacket.getSrcIP())
                .dstIP(ipPacket.getDstIP())
                .payload(ByteBufUtil.toByteBuf(ipPacket.getPayload().toByteArray()))
                .build();
        Ipv4Packet ipv4Packet = new Ipv4Packet(packet);
        IpLayerPacket ipLayerPacket = new IpLayerPacket(ipv4Packet.encode());
        sendIpLayerPacket(ipLayerPacket);
    }

    protected void install() throws Exception {
        nodeInfoMap.clear();
        sdWanClient.start();
        SDWanProtos.ServerConfigResp configResp = sdWanClient.getConfig(config.getConnectTimeout()).get();
        config.setStunServerList(configResp.getStunServersList());
        config.setRelayServerList(configResp.getRelayServersList());
        iceClient.start();
        log.info("SdWanNode install");
        String localAddress = config.getLocalAddress();
        if (null == localAddress) {
            localAddress = config.getHostAddress();
        }
        String macAddress = processMacAddress(NetworkInterfaceUtil.getHardwareAddress(localAddress));
        log.info("parseMacAddress: {}", macAddress);
        SDWanProtos.RegistReq.Builder builder = SDWanProtos.RegistReq.newBuilder()
                .setTenantId(config.getTenantId())
                .setNodeType(SDWanProtos.NodeTypeCode.SimpleType)
                .setMacAddress(macAddress)
                .setOs(PlatformUtil.normalizedOs())
                .setOsVersion(System.getProperty("os.name"));
        SDWanProtos.RegistReq registReq = builder.build();
        SDWanProtos.RegistResp regResp = sdWanClient.regist(registReq, 3000).get();
        if (!SDWanProtos.MessageCode.Success.equals(regResp.getCode())) {
            throw new ProcessCodeException(regResp.getCode().getNumber(), "registSdwan failed=" + regResp.getCode().name());
        }
        log.info("registSdwan: vip={}", regResp.getVip());
        localVip = regResp.getVip();
        maskBits = regResp.getMaskBits();
        vipCidr = regResp.getCidr();
        regResp.getNodeList().getNodeInfoList().forEach(e -> {
            nodeInfoMap.put(e.getVip(), e);
        });
        virtualRouter.updateCidr(vipCidr);
        virtualRouter.updateRoutes(mergeRouteList(regResp.getRouteList().getRouteList(), regResp.getVnatList().getVnatList()));
        virtualRouter.updateRouteRules(buildRouteRuleList(regResp.getRouteRuleList().getRouteRuleList()));
        virtualRouter.updateVNATs(regResp.getVnatList().getVnatList());
        log.info("SdWanNode installed");
        fireEvent(EventListener::onConnected);
    }

    private List<SDWanProtos.Route> mergeRouteList(List<SDWanProtos.Route> routeList, List<SDWanProtos.VNAT> vnatList) {
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
        return list;
    }

    private List<RouteRulePredicate> buildRouteRuleList(List<SDWanProtos.RouteRule> routeRuleList) {
        List<RouteRulePredicate> collect = routeRuleList.stream()
                .map(e -> new CidrRouteRulePredicate(e))
                .collect(Collectors.toList());
        return collect;
    }

    protected void uninstall() throws Exception {
        log.info("SdWanNode uninstalling");
        iceClient.stop();
        sdWanClient.stop();
    }

    protected ChannelHandler getProcessHandler() {
        return new ChannelInboundHandlerAdapter();
    }

    protected NatAddress processNatAddress(NatAddress natAddress) {
        return natAddress;
    }

    protected String processMacAddress(String hardwareAddress) {
        return hardwareAddress;
    }

    public void signalAll() throws Exception {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void await() throws Exception {
        lock.lock();
        try {
            condition.await();
        } finally {
            lock.unlock();
        }
    }

    private void fireEvent(Consumer<EventListener> consumer) {
        for (EventListener listener : eventListenerList) {
            consumer.accept(listener);
        }
    }

    @Override
    public void run() {
        boolean status = true;
        while (loopStatus.get()) {
            try {
                if (status) {
                    await();
                    status = false;
                }
                uninstall();
                install();
                status = true;
            } catch (InterruptedException e) {
            } catch (ProcessException e) {
                log.error(e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
