package io.jaspercloud.sdwan.tranport.support;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.BaseSdWanNode;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
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
public class TestSdWanNode extends BaseSdWanNode {

    public TestSdWanNode(SdWanNodeConfig config) {
        super(config);
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
                if (MessageType.Transfer.equals(stunMessage.getMessageType())) {
                    SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.parseFrom(data);
                    ByteBuf byteBuf = ByteBufUtil.toByteBuf(ipPacket.getPayload().toByteArray());
                    try {
                        Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(byteBuf);
                        byte[] bytes = ByteBufUtil.toBytes(ipv4Packet.getPayload());
                        log.info("recv transfer type={}, sender={}, src={}, dst={}, data={}",
                                transferTypeAttr.getData(), SocketAddressUtil.toAddress(sender),
                                ipPacket.getSrcIP(), ipPacket.getDstIP(), new String(bytes));
                    } finally {
                        byteBuf.release();
                    }
                }
            }
        };
    }
}
