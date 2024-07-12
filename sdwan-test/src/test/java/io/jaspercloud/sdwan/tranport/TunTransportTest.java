package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/12
 */
public class TunTransportTest {

    private TunTransport tunTransport1;
    private TunTransport tunTransport2;

    @Test
    public void test() throws Exception {
        tunTransport1 = new TunTransport(TunTransportConfig.builder()
                .ip("10.5.0.11")
                .maskBits(24)
                .tunName("tun1")
                .mtu(1500)
                .build(), () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                if (null == tunTransport2 || !tunTransport2.isRunning()) {
                    return;
                }
                Ipv4Packet ipPacket = Ipv4Packet.decodeMark(msg);
                if (!Cidr.contains("10.5.0.0/24", ipPacket.getDstIP())) {
                    return;
                }
                System.out.println(String.format("%s->%s", ipPacket.getSrcIP(), ipPacket.getDstIP()));
                SDWanProtos.IpPacket packet = SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP(ipPacket.getSrcIP())
                        .setDstIP(ipPacket.getDstIP())
                        .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(msg)))
                        .build();
                tunTransport2.writeIpPacket(packet);
            }
        });
        tunTransport1.start();
        tunTransport2 = new TunTransport(TunTransportConfig.builder()
                .ip("10.5.0.12")
                .maskBits(24)
                .tunName("tun2")
                .mtu(1500)
                .build(), () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                if (null == tunTransport1 || !tunTransport1.isRunning()) {
                    return;
                }
                Ipv4Packet ipPacket = Ipv4Packet.decodeMark(msg);
                if (!Cidr.contains("10.5.0.0/24", ipPacket.getDstIP())) {
                    return;
                }
                System.out.println(String.format("%s->%s", ipPacket.getSrcIP(), ipPacket.getDstIP()));
                SDWanProtos.IpPacket packet = SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP(ipPacket.getSrcIP())
                        .setDstIP(ipPacket.getDstIP())
                        .setPayload(ByteString.copyFrom(ByteBufUtil.toBytes(msg)))
                        .build();
                tunTransport1.writeIpPacket(packet);
            }
        });
        tunTransport2.start();
        System.out.println("start");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
