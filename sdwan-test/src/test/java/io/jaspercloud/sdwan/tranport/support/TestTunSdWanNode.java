package io.jaspercloud.sdwan.tranport.support;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.BaseSdWanNode;
import io.jaspercloud.sdwan.SdWanNodeConfig;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.route.RouteManager;
import io.jaspercloud.sdwan.route.WindowsRouteManager;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.TunChannelConfig;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author jasper
 * @create 2024/7/9
 */
@Slf4j
public class TestTunSdWanNode extends BaseSdWanNode {

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
    }

    @Override
    protected void init() throws Exception {
        super.init();
        BaseSdWanNode sdWanNode = this;
        DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, 1500)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("TunEngine:readTun", new SimpleChannelInboundHandler<ByteBuf>() {
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
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress("net-thunder", getLocalVip(), getMaskBits()));
        log.info("tun address={}", getLocalVip());
        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
        RouteManager routeManager = new WindowsRouteManager();
        for (SDWanProtos.Route route : getRouteList()) {
            routeManager.addRoute(tunChannel, route);
        }
    }
}
