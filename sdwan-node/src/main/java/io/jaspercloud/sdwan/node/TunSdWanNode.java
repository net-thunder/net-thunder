package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.processor.PingPacketProcessor;
import io.jaspercloud.sdwan.route.RouteManager;
import io.jaspercloud.sdwan.route.RouteManagerFactory;
import io.jaspercloud.sdwan.route.VirtualRouter;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tranport.TunTransport;
import io.jaspercloud.sdwan.tranport.TunTransportConfig;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessor;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessorPipeline;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author jasper
 * @create 2024/7/12
 */
@Slf4j
public class TunSdWanNode extends BaseSdWanNode {

    private SdWanNodeConfig config;

    private TunTransport tunTransport;
    private RouteManager routeManager;
    private IpLayerPacketProcessorPipeline ipPacketProcessor = new IpLayerPacketProcessorPipeline();

    public SdWanNodeConfig getConfig() {
        return config;
    }

    public void addIpLayerPacketProcessor(IpLayerPacketProcessor processor) {
        ipPacketProcessor.add(processor);
    }

    public TunSdWanNode(SdWanNodeConfig config) {
        super(config);
        this.config = config;
        addIpLayerPacketProcessor(new PingPacketProcessor());
    }

    @Override
    protected ChannelHandler getProcessHandler() {
        return new SimpleChannelInboundHandler<StunPacket>() {
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
                if (config.getShowTUNLog()) {
                    log.info("recvP2P: type={}, sender={}, src={}, dst={}",
                            transferTypeAttr.getData(), SocketAddressUtil.toAddress(sender),
                            ipPacket.getSrcIP(), ipPacket.getDstIP());
                }
                if (null == tunTransport || !tunTransport.isRunning()) {
                    return;
                }
                VirtualRouter virtualRouter = getVirtualRouter();
                ByteBuf byteBuf = ByteBufUtil.toByteBuf(ipPacket.getPayload().toByteArray());
                try {
                    IpLayerPacket packet = new IpLayerPacket(byteBuf);
                    packet = virtualRouter.routeIn(packet);
                    if (null == packet) {
                        return;
                    }
                    ipPacketProcessor.input(packet);
                    tunTransport.writeIpLayerPacket(packet.retain());
                } finally {
                    byteBuf.release();
                }
            }
        };
    }

    @Override
    protected void install() throws Exception {
        super.install();
        TunTransportConfig tunConfig = TunTransportConfig.builder()
                .tunName(config.getTunName())
                .ip(getLocalVip())
                .maskBits(getMaskBits())
                .mtu(config.getMtu())
                .localAddress(config.getLocalAddress())
                .netMesh(config.getNetMesh())
                .build();
        tunTransport = new TunTransport(tunConfig, () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                IpLayerPacket packet = new IpLayerPacket(msg);
                if (config.getShowTUNLog()) {
                    log.info("recvTUN: src={}, dst={}", packet.getSrcIP(), packet.getDstIP());
                }
                ipPacketProcessor.output(packet);
                TunSdWanNode.this.sendIpLayerPacket(packet);
            }
        });
        tunTransport.start();
        TunChannel tunChannel = tunTransport.getChannel();
        routeManager = RouteManagerFactory.create(tunChannel, getVirtualRouter());
        routeManager.start();
        fireEvent(EventListener::onConnected);
    }

    @Override
    protected void uninstall() throws Exception {
        super.uninstall();
        routeManager.stop();
        tunTransport.stop();
    }
}
