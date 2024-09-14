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
                long s = System.nanoTime();
                ByteBuf payload = icmpPacket.getPayload();
                ByteBuf byteBuf = ByteBufUtil.create();
                byteBuf.writeLong(s);
                byteBuf.writeBytes(payload);
                icmpPacket.setPayload(byteBuf);
                payload = icmpPacket.encode();
                packet.setPayload(payload);
                payload.release();
                byteBuf.release();
                break;
            }
            case IcmpPacket.Reply: {
                ByteBuf payload = icmpPacket.getPayload();
                long s = payload.readLong();
                icmpPacket.setPayload(payload.readSlice(payload.readableBytes()));
                payload = icmpPacket.encode();
                packet.setPayload(payload);
                payload.release();
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
