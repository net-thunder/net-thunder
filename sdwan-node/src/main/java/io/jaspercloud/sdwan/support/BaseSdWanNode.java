package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.stun.NatAddress;
import io.jaspercloud.sdwan.tranport.SdWanClient;
import io.jaspercloud.sdwan.tranport.SdWanClientConfig;
import io.jaspercloud.sdwan.tranport.VirtualRouter;
import io.jaspercloud.sdwan.util.AddressType;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class BaseSdWanNode implements InitializingBean, DisposableBean, Runnable {

    private SdWanNodeConfig config;

    private IceClient iceClient;
    private SdWanClient sdWanClient;
    private NatAddress natAddress;
    private String localVip;
    private int maskBits;
    private String vipCidr;
    private VirtualRouter virtualRouter;
    private AtomicBoolean loopStatus = new AtomicBoolean(false);
    private Thread loopThread;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private Map<String, SDWanProtos.NodeInfo> nodeInfoMap = new ConcurrentHashMap<>();

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

    @Override
    public void afterPropertiesSet() throws Exception {
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
    public void destroy() throws Exception {
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
                String host = UriComponentsBuilder.fromUriString(String.format("%s://%s:%d", AddressType.HOST, address, iceClient.getP2pClient().getLocalPort())).build().toString();
                builder.addAddressUri(host);
            });
            natAddress = processNatAddress(iceClient.parseNatAddress(3000));
            log.info("parseNatAddress: type={}, address={}",
                    natAddress.getMappingType().name(), SocketAddressUtil.toAddress(natAddress.getMappingAddress()));
            String srflx = UriComponentsBuilder.fromUriString(String.format("%s://%s:%d", AddressType.SRFLX,
                    natAddress.getMappingAddress().getHostString(), natAddress.getMappingAddress().getPort()))
                    .queryParam("mappingType", natAddress.getMappingType().name()).build().toString();
            builder.addAddressUri(srflx);
        }
        String token = iceClient.registRelay(3000);
        log.info("registRelay: token={}", token);
        InetSocketAddress relayAddress = SocketAddressUtil.parse(config.getRelayServer());
        String relay = UriComponentsBuilder.fromUriString(String.format("%s://%s:%d", AddressType.RELAY,
                relayAddress.getHostString(), relayAddress.getPort()))
                .queryParam("token", token).build().toString();
        builder.addAddressUri(relay);
        SDWanProtos.RegistResp regResp = sdWanClient.regist(builder.build(), 3000).get();
        log.info("registSdwan: vip={}", regResp.getVip());
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
