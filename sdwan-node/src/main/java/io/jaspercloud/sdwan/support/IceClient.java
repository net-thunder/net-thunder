package io.jaspercloud.sdwan.support;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tranport.P2pClient;
import io.jaspercloud.sdwan.tranport.RelayClient;
import io.jaspercloud.sdwan.tranport.TransportLifecycle;
import io.jaspercloud.sdwan.util.AddressType;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/5
 */
@Slf4j
public class IceClient implements TransportLifecycle {

    private SdWanNodeConfig config;
    private BaseSdWanNode sdWanNode;
    private Supplier<ChannelHandler> handler;

    private KeyPair encryptionKeyPair;
    private P2pClient p2pClient;
    private RelayClient relayClient;
    private P2pTransportManager p2pTransportManager;

    public KeyPair getEncryptionKeyPair() {
        return encryptionKeyPair;
    }

    public P2pClient getP2pClient() {
        return p2pClient;
    }

    public P2pTransportManager getP2pTransportManager() {
        return p2pTransportManager;
    }

    public IceClient(SdWanNodeConfig config, BaseSdWanNode sdWanNode, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.sdWanNode = sdWanNode;
        this.handler = handler;
    }

    public NatAddress parseNatAddress(int timeout) throws Exception {
        return p2pClient.parseNatAddress(timeout);
    }

    public String registRelay(int timeout) throws Exception {
        return relayClient.regist(timeout).get();
    }

    public void sendNode(String srcVip, SDWanProtos.NodeInfo nodeInfo, byte[] bytes) {
        AtomicReference<Transport> ref = p2pTransportManager.getOrCreate(nodeInfo.getVip(), key -> {
            election(nodeInfo)
                    .whenComplete((transport, ex) -> {
                        if (null != ex) {
                            p2pTransportManager.deleteTransport(nodeInfo.getVip());
                            return;
                        }
                        p2pTransportManager.addTransport(nodeInfo.getVip(), transport);
                        transport.transfer(srcVip, bytes);
                    });
        });
        Transport transport = ref.get();
        if (null == transport) {
            //fixme election
            return;
        }
        transport.transfer(srcVip, bytes);
    }

    private CompletableFuture<Transport> election(SDWanProtos.NodeInfo nodeInfo) {
        log.debug("election: vip={}", nodeInfo.getVip());
        List<UriComponents> uriList = nodeInfo.getAddressUriList()
                .stream()
                .map(u -> UriComponentsBuilder.fromUriString(u).build())
                .collect(Collectors.toList());
        if (config.isOnlyRelayTransport()) {
            uriList = uriList.stream()
                    .filter(uri -> AddressType.RELAY.equals(uri.getScheme()))
                    .collect(Collectors.toList());
        }
        CompletableFuture<Transport> future = AsyncTask.create(3000);
        CountBarrier<Transport> countBarrier = new CountBarrier<>(uriList.size(), list -> {
            if (list.isEmpty()) {
                throw new ProcessException("not found transport");
            }
            Optional<Transport> optional = list.stream().filter(e -> e instanceof P2pTransport).findFirst();
            if (!optional.isPresent()) {
                optional = list.stream().filter(e -> e instanceof RelayTransport).findFirst();
            }
            Transport transport = optional.get();
            future.complete(transport);
        });
        uriList.parallelStream().forEach(uri -> {
            CompletableFuture<Transport> f;
            if (AddressType.RELAY.equals(uri.getScheme())) {
                f = parseRelayTransport(nodeInfo, uri);
            } else if (AddressType.HOST.equals(uri.getScheme()) || AddressType.SRFLX.equals(uri.getScheme())) {
                f = parseP2pTransport(nodeInfo, uri);
            } else {
                throw new UnsupportedOperationException();
            }
            f.whenComplete((transport, ex) -> {
                try {
                    if (null != transport) {
                        countBarrier.add(transport);
                    }
                } finally {
                    countBarrier.countDown();
                }
            });
        });
        return future;
    }

    private CompletableFuture<Transport> parseRelayTransport(SDWanProtos.NodeInfo nodeInfo, UriComponents uri) {
        String token = uri.getQueryParams().getFirst("token");
        CompletableFuture<StunPacket> pingFuture = relayClient.ping(token, 3000);
        SDWanProtos.P2pOffer p2pOfferReq = SDWanProtos.P2pOffer.newBuilder()
                .setType(uri.getScheme())
                .setSrcVIP(sdWanNode.getLocalVip())
                .setDstVIP(nodeInfo.getVip())
                .setOfferData(relayClient.getCurToken())
                .setAnswerData(token)
                .setEncryptionKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                .build();
        return sdWanNode.getSdWanClient().offer(p2pOfferReq, 3000)
                .thenApply(resp -> {
                    try {
                        SDWanProtos.MessageCode code = resp.getCode();
                        if (!SDWanProtos.MessageCode.Success.equals(code)) {
                            throw new ProcessException("offer failed");
                        }
                        //ping success
                        pingFuture.get();
                        byte[] encryptionKey = resp.getEncryptionKey().toByteArray();
                        SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), encryptionKey);
                        return new RelayTransport(relayClient, token, secretKey);
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    private CompletableFuture<Transport> parseP2pTransport(SDWanProtos.NodeInfo nodeInfo, UriComponents uri) {
        InetSocketAddress addr = new InetSocketAddress(uri.getHost(), uri.getPort());
        CompletableFuture<StunPacket> pingFuture = p2pClient.ping(addr, 3000);
        SDWanProtos.P2pOffer p2pOfferReq = SDWanProtos.P2pOffer.newBuilder()
                .setType(uri.getScheme())
                .setSrcVIP(sdWanNode.getLocalVip())
                .setDstVIP(nodeInfo.getVip())
                .setOfferData(SocketAddressUtil.toAddress(sdWanNode.getMappingAddress().getMappingAddress().getHostString(), p2pClient.getLocalPort()))
                .setAnswerData(SocketAddressUtil.toAddress(uri.getHost(), uri.getPort()))
                .setEncryptionKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                .build();
        return sdWanNode.getSdWanClient().offer(p2pOfferReq, 3000)
                .thenApply(resp -> {
                    try {
                        SDWanProtos.MessageCode code = resp.getCode();
                        if (!SDWanProtos.MessageCode.Success.equals(code)) {
                            throw new ProcessException("offer failed");
                        }
                        InetSocketAddress sender = pingFuture.get().sender();
                        byte[] encryptionKey = resp.getEncryptionKey().toByteArray();
                        SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), encryptionKey);
                        return new P2pTransport(p2pClient, sender, secretKey);
                    } catch (Exception e) {
                        throw new ProcessException(e.getMessage(), e);
                    }
                });
    }

    public CompletableFuture<SDWanProtos.P2pAnswer> processOffer(SDWanProtos.P2pOffer p2pOffer) {
        if (AddressType.RELAY.equals(p2pOffer.getType())) {
            String token = p2pOffer.getOfferData();
            return relayClient.ping(token, 3000)
                    .handle((resp, ex) -> {
                        try {
                            SDWanProtos.MessageCode code;
                            if (null == ex) {
                                code = SDWanProtos.MessageCode.Success;
                                byte[] encryptionKey = p2pOffer.getEncryptionKey().toByteArray();
                                SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), encryptionKey);
                                Transport transport = new RelayTransport(relayClient, token, secretKey);
                                p2pTransportManager.addTransport(p2pOffer.getSrcVIP(), transport);
                            } else {
                                code = SDWanProtos.MessageCode.Failed;
                            }
                            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                                    .setCode(code)
                                    .setSrcVIP(p2pOffer.getDstVIP())
                                    .setDstVIP(p2pOffer.getSrcVIP())
                                    .setEncryptionKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                                    .build();
                            return p2pAnswer;
                        } catch (Exception e) {
                            throw new ProcessException(e.getMessage(), e);
                        }
                    });
        } else {
            InetSocketAddress remoteAddress = SocketAddressUtil.parse(p2pOffer.getOfferData());
            return p2pClient.ping(remoteAddress, 3000)
                    .handle((resp, ex) -> {
                        try {
                            SDWanProtos.MessageCode code;
                            if (null == ex) {
                                code = SDWanProtos.MessageCode.Success;
                                InetSocketAddress sender = resp.sender();
                                byte[] encryptionKey = p2pOffer.getEncryptionKey().toByteArray();
                                SecretKey secretKey = Ecdh.generateAESKey(encryptionKeyPair.getPrivate(), encryptionKey);
                                Transport transport = new P2pTransport(p2pClient, sender, secretKey);
                                p2pTransportManager.addTransport(p2pOffer.getSrcVIP(), transport);
                            } else {
                                code = SDWanProtos.MessageCode.Failed;
                            }
                            SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.newBuilder()
                                    .setCode(code)
                                    .setSrcVIP(p2pOffer.getDstVIP())
                                    .setDstVIP(p2pOffer.getSrcVIP())
                                    .setEncryptionKey(ByteString.copyFrom(encryptionKeyPair.getPublic().getEncoded()))
                                    .build();
                            return p2pAnswer;
                        } catch (Exception e) {
                            throw new ProcessException(e.getMessage(), e);
                        }
                    });
        }
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

    @Override
    public boolean isRunning() {
        return relayClient.isRunning();
    }

    @Override
    public void start() throws Exception {
        encryptionKeyPair = Ecdh.generateKeyPair();
        p2pClient = new P2pClient(config.getStunServer(), config.getP2pPort(), config.getP2pHeartTime(),
                () -> new ChannelInitializer<Channel>() {
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
                                    Transport transport = p2pTransportManager.get(srcVip);
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
                        });
                        pipeline.addLast(handler.get());
                    }
                });
        relayClient = new RelayClient(config.getRelayServer(), config.getRelayPort(), config.getP2pHeartTime(), () -> new ChannelInitializer<Channel>() {
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
                            Transport transport = p2pTransportManager.get(srcVip);
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
                });
                pipeline.addLast(handler.get());
            }
        });
        p2pTransportManager = new P2pTransportManager(config.getP2pHeartTime());
        p2pTransportManager.start();
        p2pClient.start();
        relayClient.start();
        log.info("IceClient started");
    }

    @Override
    public void stop() throws Exception {
        log.info("IceClient stopping");
        if (null != p2pTransportManager) {
            p2pTransportManager.stop();
        }
        if (null != p2pClient) {
            p2pClient.stop();
        }
        if (null != relayClient) {
            relayClient.stop();
        }
        log.info("IceClient stopped");
    }

    public interface Transport {

        void ping(long timeout) throws Exception;

        void transfer(String vip, byte[] bytes);

        byte[] decode(byte[] bytes);
    }

    private static class P2pTransport implements Transport {

        private P2pClient p2pClient;
        private InetSocketAddress address;
        private SecretKey secretKey;

        public P2pTransport(P2pClient p2pClient, InetSocketAddress address, SecretKey secretKey) {
            this.p2pClient = p2pClient;
            this.address = address;
            this.secretKey = secretKey;
        }

        @Override
        public void ping(long timeout) throws Exception {
            p2pClient.ping(address, timeout).get();
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                p2pClient.transfer(vip, address, encodeData);
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }

        @Override
        public byte[] decode(byte[] bytes) {
            try {
                byte[] decodeData = Ecdh.decryptAES(bytes, secretKey);
                return decodeData;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }
    }

    private static class RelayTransport implements Transport {

        private RelayClient relayClient;
        private String token;
        private SecretKey secretKey;

        public RelayTransport(RelayClient relayClient, String token, SecretKey secretKey) {
            this.relayClient = relayClient;
            this.token = token;
            this.secretKey = secretKey;
        }

        @Override
        public void ping(long timeout) throws Exception {
            relayClient.ping(token, timeout).get();
        }

        @Override
        public void transfer(String vip, byte[] bytes) {
            try {
                byte[] encodeData = Ecdh.encryptAES(bytes, secretKey);
                relayClient.transfer(vip, token, encodeData);
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }

        @Override
        public byte[] decode(byte[] bytes) {
            try {
                byte[] decodeData = Ecdh.decryptAES(bytes, secretKey);
                return decodeData;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        }
    }
}
