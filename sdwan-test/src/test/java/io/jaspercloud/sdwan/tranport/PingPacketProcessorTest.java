package io.jaspercloud.sdwan.tranport;

import cn.hutool.core.util.HexUtil;
import io.jaspercloud.sdwan.node.processor.PingPacketProcessor;
import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

public class PingPacketProcessorTest {

    @Test
    public void test() {
        String data = "4500004e5b34400040015058c0de0043c0a80d59" +
                "0800a78a71400001f2dbf26600000000cebd090000000000101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f3031";
        PingPacketProcessor processor = new PingPacketProcessor();
        //
        IpLayerPacket outPacket = new IpLayerPacket(ByteBufUtil.toByteBuf(ByteBufUtil.toBytes(data)));
        processor.output(outPacket);
        Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(outPacket.rebuild());
        IcmpPacket icmpPacket = IcmpPacket.decodeMark(ipv4Packet.getPayload());
        icmpPacket.setType(IcmpPacket.Reply);
        ByteBuf encodeIcmp = icmpPacket.encode(true, false);
        ipv4Packet.setPayload(encodeIcmp);
        ByteBuf encodeIp = ipv4Packet.encode(true, false);
        //
        IpLayerPacket inPacket = new IpLayerPacket(encodeIp);
        processor.input(inPacket);
        ByteBuf payload = inPacket.getPayload();
        payload.markWriterIndex();
        payload.writerIndex(0);
        payload.writeByte(IcmpPacket.Echo);
        payload.resetWriterIndex();
        String hexStr = HexUtil.encodeHexStr(ByteBufUtil.toBytes(inPacket.rebuild()));
        boolean equals = hexStr.equals(data);
        System.out.println();
    }
}
