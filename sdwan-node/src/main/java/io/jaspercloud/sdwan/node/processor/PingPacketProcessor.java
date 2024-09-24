package io.jaspercloud.sdwan.node.processor;

import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessor;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PingPacketProcessor implements IpLayerPacketProcessor {

    private void ping(IpLayerPacket packet) {
        if (Ipv4Packet.Icmp != packet.getProtocol()) {
            return;
        }
        IcmpPacket icmpPacket = IcmpPacket.decodeMark(packet.getPayload());
        switch (icmpPacket.getType()) {
            case IcmpPacket.Echo: {
                ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
                try {
                    long s = System.nanoTime();
                    byteBuf.writeLong(s);
                    byteBuf.writeBytes(icmpPacket.getPayload());
                    icmpPacket.setPayload(byteBuf);
                    ByteBuf encode = icmpPacket.encode();
                    packet.setPayload(encode);
                    encode.release();
                } finally {
                    byteBuf.release();
                }
                break;
            }
            case IcmpPacket.Reply: {
                ByteBuf payload = icmpPacket.getPayload();
                long s = payload.readLong();
                icmpPacket.setPayload(payload.readSlice(payload.readableBytes()));
                ByteBuf encode = icmpPacket.encode();
                packet.setPayload(encode);
                encode.release();
                long e = System.nanoTime();
                log.info("ping: {}", 1.0 * (e - s) / 1000 / 1000);
                break;
            }
        }
    }

    @Override
    public void input(IpLayerPacket packet) {
        ping(packet);
    }

    @Override
    public void output(IpLayerPacket packet) {
        ping(packet);
    }
}
