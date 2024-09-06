package io.jaspercloud.sdwan;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.MD5;
import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class IcmpTest {

    @Test
    public void test() {
        String data = "4500005490844000400127e3c0de0043c0de0042";
        data += "0800498a088000017e73da660000000082470c0000000000" +
                "101112131415161718191a1b1c1d1e1f2021222324252627" +
                "28292a2b2c2d2e2f3031323334353637";
        ByteBuf input = ByteBufUtil.toByteBuf(ByteBufUtil.toBytes(data));
        Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(input);
        IcmpPacket icmpPacket = IcmpPacket.decodeMark(ipv4Packet.getPayload());
        ByteBuf payload = icmpPacket.encode();
        ipv4Packet.setPayload(payload);
        ByteBuf output = ipv4Packet.encode();
        String in = MD5.create().digestHex(ByteBufUtil.toBytes(input));
        String out = MD5.create().digestHex(ByteBufUtil.toBytes(output));
        boolean eq = StringUtils.equals(in, out);
        System.out.println();
    }

    @Test
    public void encode() {
        IcmpPacket icmpPacket = new IcmpPacket();
        icmpPacket.setType(IcmpPacket.Echo);
        icmpPacket.setCode((byte) 0);
        icmpPacket.setIdentifier(RandomUtils.nextInt(1000, 5000));
        icmpPacket.setSequence(RandomUtils.nextInt(1000, 5000));
        icmpPacket.setTimestamp(System.currentTimeMillis());
        ByteBuf payload = ByteBufUtil.newPacketBuf();
        payload.writeLong(System.currentTimeMillis());
        icmpPacket.setPayload(payload);
        ByteBuf icmpEncode = icmpPacket.encode();
        Ipv4Packet ipv4Packet = new Ipv4Packet();
        ipv4Packet.setVersion((byte) 4);
        ipv4Packet.setDiffServices((byte) 0);
        ipv4Packet.setIdentifier(RandomUtils.nextInt(1000, 5000));
        ipv4Packet.setFlags((byte) 0);
        ipv4Packet.setLiveTime((short) 128);
        ipv4Packet.setProtocol(Ipv4Packet.Icmp);
        ipv4Packet.setSrcIP("192.222.0.66");
        ipv4Packet.setDstIP("192.222.0.65");
        ipv4Packet.setPayload(icmpEncode);
        ByteBuf ipEncode = ipv4Packet.encode();
        IpLayerPacket packet = new IpLayerPacket(ipEncode);
        int protocol = packet.getProtocol();
        boolean eq = Ipv4Packet.Icmp == protocol;
        ByteBuf icmpByteBuf = packet.getPayload();
        IcmpPacket decodeIcmpPacket = IcmpPacket.decodeMark(icmpByteBuf);
        {
            ByteBuf byteBuf = ByteBufUtil.create();
            byteBuf.writeLong(5555);
            byteBuf.writeBytes(decodeIcmpPacket.getPayload());
            decodeIcmpPacket.setPayload(byteBuf);
        }
        packet.setPayload(decodeIcmpPacket.encode());
        ByteBuf rebuild = packet.rebuild();
        Ipv4Packet ipv4 = Ipv4Packet.decodeMark(rebuild);
        IcmpPacket icmp = IcmpPacket.decodeMark(ipv4.getPayload());
        long time = icmp.getPayload().readLong();
        System.out.println();
    }

    @Test
    public void check() {
        String ip = "45000054f24500004001af5a0a05000cc0a80e50";
        String icmpHead = "08005fa1e39a000066dab9080000a9dd";
        String icmpData = "08090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f3031323334353637";
        IcmpPacket icmpPacket = new IcmpPacket();
        icmpPacket.setType(IcmpPacket.Echo);
        icmpPacket.setCode((byte) 0);
        icmpPacket.setIdentifier(0xe39a);
        icmpPacket.setSequence(0);
        icmpPacket.setTimestamp(7411439580802492893L);
        icmpPacket.setPayload(ByteBufUtil.toByteBuf(ByteBufUtil.toBytes(icmpData)));
        ByteBuf icmpEncode = icmpPacket.encode();
        {
            icmpEncode.markReaderIndex();
            byte[] bytes = ByteBufUtil.toBytes(icmpEncode);
            String hexStr = HexUtil.encodeHexStr(bytes);
            boolean eq = hexStr.equals(icmpHead + icmpData);
            icmpEncode.resetReaderIndex();
            System.out.println();
        }
        Ipv4Packet ipv4Packet = new Ipv4Packet();
        ipv4Packet.setVersion((byte) 4);
        ipv4Packet.setDiffServices((byte) 0);
        ipv4Packet.setIdentifier(0xf245);
        ipv4Packet.setFlags((byte) 0);
        ipv4Packet.setLiveTime((short) 64);
        ipv4Packet.setProtocol(Ipv4Packet.Icmp);
        ipv4Packet.setSrcIP("10.5.0.12");
        ipv4Packet.setDstIP("192.168.14.80");
        ipv4Packet.setPayload(icmpEncode);
        ByteBuf ipEncode = ipv4Packet.encode();
        {
            byte[] bytes = ByteBufUtil.toBytes(ipEncode);
            String hexStr = HexUtil.encodeHexStr(bytes);
            boolean eq = hexStr.equals(ip + icmpHead + icmpData);
            System.out.println();
        }
        System.out.println();
    }
}
