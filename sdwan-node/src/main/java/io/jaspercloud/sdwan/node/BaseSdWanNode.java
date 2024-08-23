package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.NatAddress;
import io.jaspercloud.sdwan.support.AddressUri;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.Lifecycle;
import io.jaspercloud.sdwan.tranport.SdWanClient;
import io.jaspercloud.sdwan.tranport.SdWanClientConfig;
import io.jaspercloud.sdwan.tranport.VirtualRouter;
import io.jaspercloud.sdwan.util.*;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class BaseSdWanNode implements Lifecycle, Runnable {

    private SdWanNodeConfig config;

    private IceClient iceClient;
    private SdWanClient sdWanClient;
    private NatAddress natAddress;
    private List<String> localAddressUriList;
    private String localVip;
    private int maskBits;
    private String vipCidr;
    private VirtualRouter virtualRouter;
    private AtomicBoolean loopStatus = new AtomicBoolean(false);
    private Thread loopThread;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private Map<String, SDWanProtos.NodeInfo> nodeInfoMap = new ConcurrentHashMap<>();

    public List<String> getLocalAddressUriList() {
        return localAddressUriList;
    }

    public NatAddress getNatAddress() {
        return natAddress;
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

    public BaseSdWanNode(SdWanNodeConfig config) {
        this.config = config;
    }

    static {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
    }

    @Override
    public void start() throws Exception {
        CheckAdmin.check();
        sdWanClient = new SdWanClient(SdWanClientConfig.builder()
                .controllerServer(config.getControllerServer())
                .connectTimeout(config.getConnectTimeout())
                .heartTime(config.getHeartTime())
                .build(),
                () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
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
                            case SDWanProtos.MessageTypeCode.NodeOnlineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                nodeInfoMap.put(nodeInfo.getVip(), nodeInfo);
                                log.debug("onlineNode: vip={}", nodeInfo.getVip());
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeOfflineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                nodeInfoMap.remove(nodeInfo.getVip());
                                log.debug("offlineNode: vip={}", nodeInfo.getVip());
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
                pipeline.addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        signalAll();
                        ctx.fireChannelInactive();
                    }
                });
            }
        });
        virtualRouter = new VirtualRouter();
        install();
        log.info("SdWanNode started");
        loopStatus.set(true);
        loopThread = new Thread(this, "loop");
        loopThread.start();
    }

    @Override
    public void stop() throws Exception {
        log.info("SdWanNode stopping");
        loopStatus.set(false);
        loopThread.interrupt();
        uninstall();
        log.info("SdWanNode stopped");
    }

    public void sendIpPacket(SDWanProtos.IpPacket ipPacket) {
        sendTo(localVip, ipPacket.getDstIP(), ipPacket.toByteArray());
    }

    private void sendTo(String srcVip, String dstIp, byte[] bytes) {
        String dstVip = virtualRouter.route(dstIp);
        if (null == dstVip) {
            return;
        }
        if (Cidr.isBroadcastAddress(vipCidr, dstVip)) {
            nodeInfoMap.values().forEach(nodeInfo -> {
                iceClient.sendNode(srcVip, nodeInfo, bytes);
            });
            return;
        }
        SDWanProtos.NodeInfo nodeInfo = nodeInfoMap.get(dstVip);
        if (null == nodeInfo) {
            return;
        }
        iceClient.sendNode(srcVip, nodeInfo, bytes);
    }

    protected void install() throws Exception {
        nodeInfoMap.clear();
        iceClient.start();
        sdWanClient.start();
        log.info("SdWanNode install");
        SDWanProtos.RegistReq.Builder builder = SDWanProtos.RegistReq.newBuilder()
                .setNodeType(SDWanProtos.NodeTypeCode.SimpleType)
                .setMacAddress(processMacAddress(NetworkInterfaceUtil.getHardwareAddress()));
        if (!config.isOnlyRelayTransport()) {
            List<NetworkInterfaceInfo> interfaceInfoList;
            if (null != config.getLocalAddress()) {
                NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(config.getLocalAddress());
                interfaceInfoList = Arrays.asList(networkInterfaceInfo);
            } else {
                InetAddress[] inetAddresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
                interfaceInfoList = NetworkInterfaceUtil.parseInetAddress(inetAddresses);
            }
            interfaceInfoList.forEach(e -> {
                String address = e.getInterfaceAddress().getAddress().getHostAddress();
                String host = AddressUri.builder()
                        .scheme(AddressType.HOST)
                        .host(address)
                        .port(iceClient.getP2pClient().getLocalPort())
                        .build().toString();
                builder.addAddressUri(host);
            });
            natAddress = processNatAddress(iceClient.parseNatAddress(3000));
            log.info("parseNatAddress: type={}, address={}",
                    natAddress.getMappingType().name(), SocketAddressUtil.toAddress(natAddress.getMappingAddress()));
            String srflx = AddressUri.builder()
                    .scheme(AddressType.SRFLX)
                    .host(natAddress.getMappingAddress().getHostString())
                    .port(natAddress.getMappingAddress().getPort())
                    .params(Collections.singletonMap("mappingType", natAddress.getMappingType().name()))
                    .build().toString();
            builder.addAddressUri(srflx);
        }
        String token = iceClient.registRelay(3000);
        log.info("registRelay: token={}", token);
        InetSocketAddress relayAddress = SocketAddressUtil.parse(config.getRelayServer());
        String relay = AddressUri.builder()
                .scheme(AddressType.RELAY)
                .host(relayAddress.getHostString())
                .port(relayAddress.getPort())
                .params(Collections.singletonMap("token", token))
                .build().toString();
        builder.addAddressUri(relay);
        SDWanProtos.RegistReq registReq = builder.build();
        SDWanProtos.RegistResp regResp = sdWanClient.regist(registReq, 3000).get();
        if (!SDWanProtos.MessageCode.Success.equals(regResp.getCode())) {
            throw new ProcessException("registSdwan failed=" + regResp.getCode().name());
        }
        log.info("registSdwan: vip={}", regResp.getVip());
        localAddressUriList = registReq.getAddressUriList().stream().collect(Collectors.toList());
        localVip = regResp.getVip();
        maskBits = regResp.getMaskBits();
        vipCidr = Cidr.parseCidr(regResp.getVip(), maskBits);
        regResp.getNodeList().getNodeInfoList().forEach(e -> {
            nodeInfoMap.put(e.getVip(), e);
        });
        virtualRouter.updateCidr(vipCidr);
        virtualRouter.updateRoutes(regResp.getRouteList().getRouteList());
        log.info("SdWanNode installed");
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
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
