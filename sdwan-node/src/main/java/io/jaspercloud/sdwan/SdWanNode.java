package io.jaspercloud.sdwan;

import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.util.AddressType;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.SdWanClient;
import io.jaspercloud.sdwan.tranport.SdWanClientConfig;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class SdWanNode implements InitializingBean, Runnable {

    private SdWanNodeConfig config;

    private IceClient iceClient;
    private SdWanClient sdWanClient;
    private MappingAddress mappingAddress;
    private String localVip;
    private String vipCidr;
    private List<SDWanProtos.Route> routeList;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private AtomicReference<Map<String, SDWanProtos.NodeInfo>> nodeInfoMapRef = new AtomicReference<>();

    public MappingAddress getMappingAddress() {
        return mappingAddress;
    }

    public String getLocalVip() {
        return localVip;
    }

    public SdWanClient getSdWanClient() {
        return sdWanClient;
    }

    public IceClient getIceClient() {
        return iceClient;
    }

    public SdWanNode(SdWanNodeConfig config) {
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
                                SDWanProtos.SocketAddress address = p2pOffer.getOfferAddress();
                                InetSocketAddress remoteAddress = new InetSocketAddress(address.getIp(), address.getPort());
                                iceClient.ping(p2pOffer.getSrcVIP(), remoteAddress, 1000)
                                        .whenComplete((result, ex) -> {
                                            SDWanProtos.MessageCode code;
                                            if (null == ex) {
                                                code = SDWanProtos.MessageCode.Success;
                                            } else {
                                                code = SDWanProtos.MessageCode.Failed;
                                            }
                                            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                                                    .setCode(code)
                                                    .setSrcVIP(p2pOffer.getDstVIP())
                                                    .setDstVIP(p2pOffer.getSrcVIP())
                                                    .build();
                                            sdWanClient.answer(msg.getReqId(), p2pAnswer);
                                        });
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.P2pAnswerType_VALUE: {
                                AsyncTask.completeTask(msg.getReqId(), msg);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeInfoListType_VALUE: {
                                SDWanProtos.NodeInfoList nodeInfoList = SDWanProtos.NodeInfoList.parseFrom(msg.getData());
                                Map<String, SDWanProtos.NodeInfo> map = nodeInfoList.getNodeInfoList().stream().collect(Collectors.toMap(e -> e.getVip(), e -> e));
                                nodeInfoMapRef.set(map);
                                break;
                            }
                        }
                    }
                });
        iceClient = new IceClient(config, this, () -> new SimpleChannelInboundHandler<StunPacket>() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                signalAll();
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket msg) throws Exception {
                InetSocketAddress sender = msg.sender();
                StunMessage stunMessage = msg.content();
                StringAttr transferTypeAttr = stunMessage.getAttr(AttrType.TransferType);
                AddressAttr addressAttr = stunMessage.getAttr(AttrType.SourceAddress);
                BytesAttr dataAttr = stunMessage.getAttr(AttrType.Data);
                byte[] data = dataAttr.getData();
                if (MessageType.Transfer.equals(stunMessage.getMessageType())) {
                    log.info("transfer type={}, src={}, sender={}, data={}",
                            transferTypeAttr.getData(), SocketAddressUtil.toAddress(addressAttr.getAddress()), SocketAddressUtil.toAddress(sender), new String(data));
                }
            }
        });
        init();
        new Thread(this, "loop").start();
    }

    public void sendIpPacket(SDWanProtos.IpPacket ipPacket) {
        sendTo(ipPacket.getDstIP(), ipPacket.getData().toByteArray());
    }

    private void sendTo(String dstIp, byte[] bytes) {
        String dstVip = findNexthop(dstIp);
        if (null == dstVip) {
            return;
        }
        SDWanProtos.NodeInfo nodeInfo = nodeInfoMapRef.get().get(dstVip);
        if (null == nodeInfo) {
            return;
        }
        iceClient.sendNode(nodeInfo, bytes);
    }

    private String findNexthop(String ip) {
        for (SDWanProtos.Route route : routeList) {
            if (Cidr.contains(route.getDestination(), ip)) {
                ProtocolStringList nexthopList = route.getNexthopList();
                if (nexthopList.isEmpty()) {
                    return null;
                }
                int rand = RandomUtils.nextInt(0, nexthopList.size());
                String nexthop = nexthopList.get(rand);
                return nexthop;
            }
        }
        if (Cidr.contains(vipCidr, ip)) {
            return ip;
        }
        return null;
    }

    private void init() throws Exception {
        iceClient.start();
        sdWanClient.start();
        SDWanProtos.RegistReq.Builder builder = SDWanProtos.RegistReq.newBuilder()
                .setNodeType(SDWanProtos.NodeTypeCode.SimpleType)
                .setMacAddress(processMacAddress(NetworkInterfaceUtil.getHardwareAddress()));
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
            String host = UriComponentsBuilder.fromUriString(String.format("%s://%s:%d", AddressType.HOST, address, config.getP2pPort())).build().toString();
            builder.addAddressUri(host);
        });
        mappingAddress = processMappingAddress(iceClient.parseMappingAddress(3000));
        String token = iceClient.registRelay(3000);
        String srflx = UriComponentsBuilder.fromUriString(String.format("%s://%s:%d", AddressType.SRFLX,
                mappingAddress.getMappingAddress().getHostString(), mappingAddress.getMappingAddress().getPort()))
                .queryParam("mappingType", mappingAddress.getMappingType().name()).build().toString();
        InetSocketAddress relayAddress = SocketAddressUtil.parse(config.getRelayServer());
        String relay = UriComponentsBuilder.fromUriString(String.format("%s://%s:%d", AddressType.RELAY,
                relayAddress.getHostString(), relayAddress.getPort()))
                .queryParam("token", token).build().toString();
        builder.addAddressUri(srflx);
        builder.addAddressUri(relay);
        SDWanProtos.RegistResp regResp = sdWanClient.regist(builder.build(), 3000).get();
        localVip = regResp.getVip();
        vipCidr = Cidr.parseCidr(regResp.getVip(), regResp.getMaskBits());
        routeList = regResp.getRouteList().getRouteList();
        log.info("localVip={}", localVip);
    }

    protected MappingAddress processMappingAddress(MappingAddress mappingAddress) {
        return mappingAddress;
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
        while (true) {
            try {
                if (status) {
                    await();
                    status = false;
                }
                iceClient.stop();
                sdWanClient.stop();
                init();
                status = true;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
