package io.jaspercloud.sdwan;

import com.google.protobuf.ByteString;
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

    public TunSdWanNode(SdWanNodeConfig config) {
        super(config);
        this.config = config;
    }

    @Override
    protected ChannelHandler getTunHandler() {
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
                if (null == tunTransport || !tunTransport.isRunning()) {
                    return;
                }
                tunTransport.writeIpPacket(ipPacket);
            }
        };
    }

    @Override
    protected void init() throws Exception {
        super.init();
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
                TunSdWanNode.this.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP(ipv4Packet.getSrcIP())
                        .setDstIP(ipv4Packet.getDstIP())
                        .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(msg)))
                        .build());
            }
        });
        tunTransport.start();
        TunChannel tunChannel = tunTransport.getChannel();
        RouteManager routeManager = new WindowsRouteManager();
        for (SDWanProtos.Route route : getRouteList()) {
            routeManager.addRoute(tunChannel, route);
        }
        log.info("TunSdWanNode started");
    }
}
