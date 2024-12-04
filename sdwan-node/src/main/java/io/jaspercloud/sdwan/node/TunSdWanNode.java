package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.node.processor.PingPacketProcessor;
import io.jaspercloud.sdwan.route.OSRouteTableFactory;
import io.jaspercloud.sdwan.route.OSRouteTableManager;
import io.jaspercloud.sdwan.tranport.TunTransport;
import io.jaspercloud.sdwan.tranport.TunTransportConfig;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessor;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessorPipeline;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.util.CheckAdmin;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jasper
 * @create 2024/7/12
 */
@Slf4j
public class TunSdWanNode extends BaseSdWanNode {

    private SdWanNodeConfig config;

    private TunTransport tunTransport;
    private OSRouteTableManager osRouteTableManager;
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
    protected void onData(IpLayerPacket packet) {
        ipPacketProcessor.input(packet);
        if (tunTransport.isRunning()) {
            tunTransport.writeIpLayerPacket(packet.retain());
        }
    }

    @Override
    protected void install() throws Exception {
        CheckAdmin.check();
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
                if (config.getShowVRouterLog()) {
                    log.info("recvTUN: src={}, dst={}", packet.getSrcIP(), packet.getDstIP());
                }
                ipPacketProcessor.output(packet);
                getVirtualRouter().send(packet);
            }
        });
        tunTransport.start();
        TunChannel tunChannel = tunTransport.getChannel();
        osRouteTableManager = OSRouteTableFactory.create(tunChannel);
        osRouteTableManager.start();
        osRouteTableManager.update(getVirtualRouter().getRouteList());
        fireEvent(EventListener::onConnected);
        log.info("TunSdWanNode installed");
    }

    @Override
    protected void uninstall() throws Exception {
        log.info("TunSdWanNode uninstalling");
        if (null != osRouteTableManager) {
            osRouteTableManager.stop();
            osRouteTableManager = null;
        }
        if (null != tunTransport) {
            tunTransport.stop();
            tunTransport = null;
        }
        super.uninstall();
    }
}
