package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PingerTest {

    @Test
    public void test() throws Exception {
        Map<Integer, CompletableFuture<IcmpPacket>> futureMap = new ConcurrentHashMap<>();
        TunTransport tunTransport = new TunTransport(TunTransportConfig.builder()
                .ip("10.5.0.12")
                .maskBits(24)
                .tunName("net-thunder")
                .mtu(1440)
                .localAddress("192.222.0.66")
                .netMesh(true)
                .build(), () -> new SimpleChannelInboundHandler<ByteBuf>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(msg);
                if (Ipv4Packet.Icmp != ipv4Packet.getProtocol()) {
                    return;
                }
                IcmpPacket reply = IcmpPacket.decodeMark(ipv4Packet.getPayload());
//                System.out.println("read: " + reply.getSequence());
                CompletableFuture<IcmpPacket> future = futureMap.get(reply.getSequence());
                if (null == future) {
                    return;
                }
                future.complete(reply);
            }
        });
        tunTransport.start();
        TunChannel channel = tunTransport.getChannel();
        while (true) {
            long s = System.nanoTime();
            IcmpPacket echo = echo(s);
            Ipv4Packet request = ipv4Packet(echo);
            IpLayerPacket packet = new IpLayerPacket(request.encode());
            IcmpPacket icmpPacket = IcmpPacket.decodeMark(packet.getPayload());
            ByteBuf byteBuf = ByteBufUtil.create();
            byteBuf.writeInt(0x1100);
            byteBuf.writeLong(5555);
            byteBuf.writeBytes(icmpPacket.getPayload());
            icmpPacket.setPayload(byteBuf);
            packet.setPayload(icmpPacket.encode());
            byteBuf.release();
            CompletableFuture<IcmpPacket> future = new CompletableFuture<>();
            futureMap.put(echo.getSequence(), future);
            channel.writeAndFlush(packet.rebuild());
//            System.out.println("write: " + echo.getSequence());
            IcmpPacket reply = future.get();
            long e = System.nanoTime();
            System.out.println("time: " + (1.0 * (e - s) / 1000 / 1000));
            Thread.sleep(1000);
        }
    }

    public IcmpPacket echo(long time) {
        IcmpPacket icmpPacket = new IcmpPacket();
        icmpPacket.setType(IcmpPacket.Echo);
        icmpPacket.setCode((byte) 0);
        icmpPacket.setIdentifier(RandomUtils.nextInt(1000, 5000));
        icmpPacket.setSequence(RandomUtils.nextInt(1000, 5000));
        icmpPacket.setTimestamp(time);
        ByteBuf payload = ByteBufUtil.create();
        payload.writeLong(time);
        icmpPacket.setPayload(payload);
        return icmpPacket;
    }

    public Ipv4Packet ipv4Packet(IcmpPacket icmpPacket) {
        ByteBuf icmpEncode = icmpPacket.encode();
        Ipv4Packet ipv4Packet = new Ipv4Packet();
        ipv4Packet.setVersion((byte) 4);
        ipv4Packet.setDiffServices((byte) 0);
        ipv4Packet.setIdentifier(RandomUtils.nextInt(1000, 5000));
        ipv4Packet.setFlags((byte) 0);
        ipv4Packet.setLiveTime((short) 128);
        ipv4Packet.setProtocol(Ipv4Packet.Icmp);
        ipv4Packet.setSrcIP("10.5.0.13");
        ipv4Packet.setDstIP("192.168.14.50");
        ipv4Packet.setPayload(icmpEncode);
        return ipv4Packet;
    }
}
