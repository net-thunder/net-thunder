package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.Ecdh;
import io.jaspercloud.sdwan.tranport.*;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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
    private ElectionProtocol electionProtocol;
    private AtomicBoolean status = new AtomicBoolean(false);

    public P2pClient getP2pClient() {
        return p2pClient;
    }

    public RelayClient getRelayClient() {
        return relayClient;
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
        AtomicReference<DataTransport> ref = p2pTransportManager.getOrCreate(nodeInfo.getVip(), key -> {
            log.debug("election: vip={}", nodeInfo.getVip());
            electionProtocol.offer(nodeInfo)
                    .whenComplete((transport, ex) -> {
                        if (null != ex) {
                            log.info("offer deleteTransport: vip={}, error={}", nodeInfo.getVip(), ex.getMessage());
                            p2pTransportManager.deleteTransport(nodeInfo.getVip());
                            return;
                        }
                        log.info("offer addTransport: vip={}", nodeInfo.getVip());
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

    public void processOffer(String reqId, SDWanProtos.P2pOffer p2pOffer) {
        log.info("processOffer: id={}", reqId);
        electionProtocol.answer(reqId, p2pOffer)
                .thenAccept(transport -> {
                    log.debug("answer addTransport: vip={}", p2pOffer.getSrcVIP());
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

    private ChannelHandler createStunPacketHandler() {
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
        p2pClient = new P2pClient(config.getStunServer(), config.getP2pPort(), config.getP2pHeartTime(),
                () -> createStunPacketHandler());
        relayClient = new RelayClient(config.getRelayServer(), config.getRelayPort(), config.getP2pHeartTime(),
                () -> createStunPacketHandler());
        electionProtocol = new ElectionProtocol(config.getTenantId(), p2pClient, relayClient, encryptionKeyPair) {
            @Override
            protected CompletableFuture<SDWanProtos.P2pAnswer> sendOffer(SDWanProtos.P2pOffer p2pOffer, long timeout) {
                return sdWanNode.getSdWanClient().offer(p2pOffer, timeout);
            }

            @Override
            protected void sendAnswer(String reqId, SDWanProtos.P2pAnswer p2pAnswer) {
                sdWanNode.getSdWanClient().answer(reqId, p2pAnswer);
            }

            @Override
            protected String getLocalVip() {
                return sdWanNode.getLocalVip();
            }

            @Override
            protected List<String> getLocalAddressUriList() {
                return sdWanNode.getLocalAddressUriList();
            }
        };
        p2pTransportManager = new P2pTransportManager(config.getP2pHeartTime());
        p2pTransportManager.start();
        p2pClient.start();
        relayClient.start();
        log.info("IceClient started");
        status.set(true);
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
        status.set(false);
    }

}
