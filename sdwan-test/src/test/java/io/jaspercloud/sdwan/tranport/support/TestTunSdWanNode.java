package io.jaspercloud.sdwan.tranport.support;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.route.RouteManager;
import io.jaspercloud.sdwan.route.WindowsRouteManager;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.node.BaseSdWanNode;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.tranport.TunTransport;
import io.jaspercloud.sdwan.tranport.TunTransportConfig;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
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
 * @create 2024/7/9
 */
@Slf4j
public class TestTunSdWanNode extends BaseSdWanNode {

    private SdWanNodeConfig config;

    private TunTransport tunTransport;
    private RouteManager routeManager;

    public TestTunSdWanNode(SdWanNodeConfig config) {
        super(config);
        this.config = config;
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
                log.debug("recv transfer type={}, sender={},  src={}, dst={}",
                        transferTypeAttr.getData(), SocketAddressUtil.toAddress(sender),
                        ipPacket.getSrcIP(), ipPacket.getDstIP());
            }
        };
    }

    @Override
    protected void install() throws Exception {
        super.install();
        BaseSdWanNode sdWanNode = this;
        TunTransportConfig tunConfig = TunTransportConfig.builder()
                .tunName(config.getTunName())
                .ip(getLocalVip())
                .maskBits(getMaskBits())
                .mtu(config.getMtu())
                .build();
        tunTransport = new TunTransport(tunConfig, () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(msg);
                sdWanNode.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP(ipv4Packet.getSrcIP())
                        .setDstIP(ipv4Packet.getDstIP())
                        .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(msg)))
                        .build());
            }
        });
        tunTransport.start();
        TunChannel tunChannel = tunTransport.getChannel();
        routeManager = new WindowsRouteManager(tunChannel, getVirtualRouter());
        routeManager.start();
    }

    @Override
    protected void uninstall() throws Exception {
        tunTransport.stop();
        routeManager.stop();
        super.uninstall();
    }
}
