package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessor;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CountDownLatch;

public class SdWanNodeDebug {

    public static void main(String[] args) throws Exception {
        Logger logger = new LoggerSystem().initUserDir();
        TunSdWanNode mainSdWanNode = new TunSdWanNode(new ConfigSystem().initUserDir());
        mainSdWanNode.addIpLayerPacketProcessor(new IpLayerPacketProcessor() {
            @Override
            public void input(IpLayerPacket packet) {
                if (Ipv4Packet.Icmp != packet.getProtocol()) {
                    return;
                }

                long s = System.nanoTime();
                IcmpPacket icmpPacket = IcmpPacket.decodeMark(packet.getPayload());
                ByteBuf payload = icmpPacket.getPayload();
                ByteBuf byteBuf = ByteBufUtil.create();
                byteBuf.writeLong(s);
                byteBuf.writeBytes(payload);
                icmpPacket.setPayload(byteBuf);
                payload = icmpPacket.encode();
                packet.setPayload(payload);
                payload.release();
                byteBuf.release();
            }

            @Override
            public void output(IpLayerPacket packet) {
                if (Ipv4Packet.Icmp != packet.getProtocol()) {
                    return;
                }
                IcmpPacket icmpPacket = IcmpPacket.decodeMark(packet.getPayload());
                ByteBuf payload = icmpPacket.getPayload();
                long s = payload.readLong();
                icmpPacket.setPayload(payload.readSlice(payload.readableBytes()));
                payload = icmpPacket.encode();
                packet.setPayload(payload);
                payload.release();
            }
        });
        mainSdWanNode.start();
        logger.info("SdWanNodeDebug started");
        CountDownLatch latch = new CountDownLatch(1);
        latch.await();
    }
}
