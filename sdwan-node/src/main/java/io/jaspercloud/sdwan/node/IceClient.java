package io.jaspercloud.sdwan.node;

import cn.hutool.core.lang.Pair;
import cn.hutool.crypto.digest.DigestUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.AddressUri;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.support.Ecdh;
import io.jaspercloud.sdwan.tranport.*;
import io.jaspercloud.sdwan.util.AddressType;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/5
 */
@Slf4j
public class IceClient implements TransportLifecycle, Runnable {

    private SdWanNodeConfig config;
    private SdWanClient sdWanClient;
    private Supplier<ChannelHandler> handler;
    private String localVip;

    private KeyPair encryptionKeyPair;
    private P2pClient p2pClient;
    private RelayClient relayClient;
    private P2pTransportManager p2pTransportManager;
    private ElectionProtocol electionProtocol;
    private AtomicBoolean status = new AtomicBoolean(false);
    private volatile Pair<String, List<String>> localAddressUriListRef = new Pair<>("", Collections.emptyList());
    private ScheduledExecutorService scheduledExecutorService;

    public P2pClient getP2pClient() {
        return p2pClient;
    }

    public RelayClient getRelayClient() {
        return relayClient;
    }

    public P2pTransportManager getP2pTransportManager() {
        return p2pTransportManager;
    }

    public void applyLocalVip(String localVip) {
        this.localVip = localVip;
    }

    public IceClient(SdWanNodeConfig config, SdWanClient sdWanClient, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.sdWanClient = sdWanClient;
        this.handler = handler;
    }

    public void sendNode(String srcVip, SDWanProtos.NodeInfo nodeInfo, byte[] bytes) {
        AtomicReference<DataTransport> ref = p2pTransportManager.getOrCreate(nodeInfo.getVip(), key -> {
            electionProtocol.processOffer(nodeInfo)
                    .whenComplete((transport, ex) -> {
                        if (null != ex) {
                            log.info("electionError: vip={}, error={}", nodeInfo.getVip(), ex.getMessage());
                            p2pTransportManager.deleteTransport(nodeInfo.getVip());
                            return;
                        }
                        log.info("electionSuccess: vip={}, addressUri={}", nodeInfo.getVip(), transport.addressUri().toString());
                        p2pTransportManager.addTransport(nodeInfo.getVip(), transport);
                        transport.transfer(srcVip, bytes);
                    });
        });
        DataTransport transport = ref.get();
        if (null == transport) {
            //in the election
            return;
        }
        transport.transfer(srcVip, bytes);
    }

    public void processAnswer(String reqId, SDWanProtos.P2pOffer p2pOffer) {
        electionProtocol.processAnswer(reqId, p2pOffer)
                .thenAccept(transport -> {
                    p2pTransportManager.addTransport(p2pOffer.getSrcVIP(), transport);
                });
    }

    private void processPingRequest(ChannelHandlerContext ctx, StunPacket packet) {
        try {
            StunMessage message = packet.content();
            message.setMessageType(MessageType.PingResponse);
            StunPacket response = new StunPacket(message, packet.sender());
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    private ChannelHandler createStunPacketHandler(String clientTag) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new SimpleChannelInboundHandler<StunPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                        StunMessage request = packet.content();
                        InetSocketAddress sender = packet.sender();
                        if (MessageType.PingRequest.equals(request.getMessageType())) {
                            processPingRequest(ctx, packet);
                        } else if (MessageType.Transfer.equals(request.getMessageType())) {
                            StunMessage stunMessage = packet.content();
                            StringAttr srcVipAttr = stunMessage.getAttr(AttrType.SrcVip);
                            String srcVip = srcVipAttr.getData();
                            BytesAttr dataAttr = stunMessage.getAttr(AttrType.Data);
                            byte[] data = dataAttr.getData();
                            DataTransport transport = p2pTransportManager.get(srcVip);
                            if (null == transport) {
                                return;
                            }
                            data = transport.decode(data);
                            stunMessage.setAttr(AttrType.Data, new BytesAttr(data));
                            ctx.fireChannelRead(packet);
                        } else {
                            ctx.fireChannelRead(packet);
                        }
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        log.info("{} channelInactive", clientTag);
                        super.channelInactive(ctx);
                    }
                });
                pipeline.addLast(handler.get());
            }
        };
    }

    @Override
    public boolean isRunning() {
        return status.get();
    }

    @Override
    public void start() throws Exception {
        encryptionKeyPair = Ecdh.generateKeyPair();
        p2pClient = new P2pClient(new P2pClient.Config() {
            {
                setLocalPort(config.getP2pPort());
                setShowICELog(config.getShowICELog());
            }
        }, () -> createStunPacketHandler("p2pClient"));
        relayClient = new RelayClient(new RelayClient.Config() {
            {
                setLocalPort(config.getRelayPort());
                setShowICELog(config.getShowICELog());
            }
        }, () -> createStunPacketHandler("relayClient"));
        ElectionProtocol.Config electionConfig = new ElectionProtocol.Config() {
            {
                setTenantId(config.getTenantId());
                setEncryptionKeyPair(encryptionKeyPair);
                setElectionTimeout(config.getElectionTimeout());
                setPingTimeout(config.getP2pCheckTimeout());
                setShowElectionLog(config.getShowElectionLog());
            }
        };
        electionProtocol = new ElectionProtocol(electionConfig, p2pClient, relayClient) {
            @Override
            protected CompletableFuture<SDWanProtos.P2pAnswer> sendOffer(SDWanProtos.P2pOffer p2pOffer, long timeout) {
                return sdWanClient.offer(p2pOffer, timeout);
            }

            @Override
            protected void sendAnswer(String reqId, SDWanProtos.P2pAnswer p2pAnswer) {
                sdWanClient.answer(reqId, p2pAnswer);
            }

            @Override
            protected String getLocalVip() {
                return localVip;
            }

            @Override
            protected List<String> getLocalAddressUriList() {
                return localAddressUriListRef.getValue();
            }
        };
        p2pTransportManager = new P2pTransportManager(config);
        p2pTransportManager.start();
        p2pClient.start();
        relayClient.start();
        status.set(true);
        log.info("IceClient started");
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this, 0, config.getIceCheckTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() throws Exception {
        log.info("IceClient stopping");
        if (null != p2pTransportManager) {
            p2pTransportManager.stop();
        }
        if (null != scheduledExecutorService) {
            scheduledExecutorService.shutdown();
        }
        if (null != p2pClient) {
            p2pClient.stop();
        }
        if (null != relayClient) {
            relayClient.stop();
        }
        log.info("IceClient stopped");
        status.set(false);
    }

    @Override
    public void run() {
        if (!status.get()) {
            return;
        }
        try {
            checkServerList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void checkServerList() throws Exception {
        List<String> stunServerList = config.getStunServerList();
        List<String> relayServerList = config.getRelayServerList();
        BlockingQueue<AddressUri> queue = new LinkedBlockingQueue<>();
        CountDownLatch countDownLatch = new CountDownLatch(stunServerList.size() + relayServerList.size());
        for (String address : stunServerList) {
            try {
                checkStun(countDownLatch, address, queue);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
        for (String address : relayServerList) {
            try {
                checkRelay(countDownLatch, address, queue);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
        try {
            countDownLatch.await(config.getIceCheckTimeout(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
        List<AddressUri> addressUriList = new ArrayList<>();
        queue.drainTo(addressUriList);
        updateNodeInfo(addressUriList);
    }

    public void updateNodeInfo(List<AddressUri> addressUriList) throws Exception {
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
                if (StringUtils.equals(address, localVip)) {
                    return;
                }
                AddressUri addressUri = AddressUri.builder()
                        .scheme(AddressType.HOST)
                        .host(address)
                        .port(p2pClient.getLocalPort())
                        .build();
                list.add(addressUri);
            });
            list.addAll(addressUriList);
        }
        List<String> collect = list.stream().map(e -> e.toString()).sorted().collect(Collectors.toList());
        String md5 = DigestUtil.md5Hex(StringUtils.join(collect));
        String key = localAddressUriListRef.getKey();
        if (StringUtils.equals(md5, key)) {
            return;
        }
        localAddressUriListRef = new Pair<>(md5, collect);
        /**
         * ElectionProtocol.offer使用nodeInfo
         */
        sdWanClient.updateNodeInfo(collect);
    }

    private void checkStun(CountDownLatch countDownLatch, String address, BlockingQueue<AddressUri> queue) {
        InetSocketAddress socketAddress = SocketAddressUtil.parse(address);
        String tranId = StunMessage.genTranId();
        AsyncTask.waitTask(tranId, config.getIceCheckTimeout())
                .whenComplete((response, ex) -> {
                    try {
                        if (null != ex) {
                            log.error("ping stunServer timeout: {}", address);
                            return;
                        }
                        StunPacket packet = (StunPacket) response;
                        Map<AttrType, Attr> attrs = packet.content().getAttrs();
                        AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
                        InetSocketAddress natAddress = mappedAddressAttr.getAddress();
                        if (config.getShowICELog()) {
                            log.info("connect stunServer success: address={}, publicAddress={}", address, SocketAddressUtil.toAddress(natAddress));
                        }
                        Map<String, String> params = new HashMap<>();
                        params.put("server", address);
                        AddressUri addressUri = AddressUri.builder()
                                .scheme(AddressType.SRFLX)
                                .host(natAddress.getHostString())
                                .port(natAddress.getPort())
                                .params(params)
                                .build();
                        queue.add(addressUri);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
        p2pClient.sendBindOneWay(socketAddress, tranId);
    }

    private void checkRelay(CountDownLatch countDownLatch, String address, BlockingQueue<AddressUri> queue) {
        InetSocketAddress socketAddress = SocketAddressUtil.parse(address);
        String tranId = StunMessage.genTranId();
        AsyncTask.waitTask(tranId, config.getIceCheckTimeout())
                .whenComplete((response, ex) -> {
                    try {
                        if (null != ex) {
                            log.error("ping relayServer timeout: {}", address);
                            return;
                        }
                        StunPacket packet = (StunPacket) response;
                        StunMessage stunMessage = packet.content();
                        StringAttr attr = stunMessage.getAttr(AttrType.RelayToken);
                        String token = attr.getData();
                        if (config.getShowICELog()) {
                            log.info("connect relayServer success: address={}, token={}", address, token);
                        }
                        Map<String, String> params = new HashMap<>();
                        params.put("server", address);
                        params.put("token", token);
                        AddressUri addressUri = AddressUri.builder()
                                .scheme(AddressType.RELAY)
                                .host(socketAddress.getHostString())
                                .port(socketAddress.getPort())
                                .params(params)
                                .build();
                        queue.add(addressUri);
                    } finally {
                        countDownLatch.countDown();
                    }
                });
        relayClient.sendBindOneWay(socketAddress, tranId);
    }

    public void offlineTransport(String vip) {
        p2pTransportManager.deleteTransport(vip);
    }
}
