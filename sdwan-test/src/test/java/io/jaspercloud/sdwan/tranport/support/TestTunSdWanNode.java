package io.jaspercloud.sdwan.tranport.support;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.BaseSdWanNode;
import io.jaspercloud.sdwan.SdWanNodeConfig;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.route.RouteManager;
import io.jaspercloud.sdwan.route.WindowsRouteManager;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tranport.TunTransport;
import io.jaspercloud.sdwan.tranport.TunTransportConfig;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.buffer.ByteBuf;
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

    private TunTransportConfig config;

    public TestTunSdWanNode(SdWanNodeConfig config) {
        super(config, () -> new SimpleChannelInboundHandler<StunPacket>() {
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
        this.config = TunTransportConfig.builder()
                .tunName(config.getTunName())
                .ip(config.getIp())
                .maskBits(config.getMaskBits())
                .mtu(config.getMtu())
                .build();
    }

    @Override
    protected void init() throws Exception {
        super.init();
        BaseSdWanNode sdWanNode = this;
        config.setIp(getLocalVip());
        config.setMaskBits(getMaskBits());
        TunTransport tunTransport = new TunTransport(config, () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(msg);
                sdWanNode.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP(ipv4Packet.getSrcIP())
                        .setDstIP(ipv4Packet.getDstIP())
                        .setData(ByteString.copyFrom(ByteBufUtil.toBytes(msg)))
                        .build());
            }
        });
        tunTransport.start();
        TunChannel tunChannel = tunTransport.getChannel();
        RouteManager routeManager = new WindowsRouteManager();
        for (SDWanProtos.Route route : getRouteList()) {
            routeManager.addRoute(tunChannel, route);
        }
    }
}
