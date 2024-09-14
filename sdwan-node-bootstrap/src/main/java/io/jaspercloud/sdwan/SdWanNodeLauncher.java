package io.jaspercloud.sdwan;

import ch.qos.logback.classic.Logger;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.LoggerSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.platform.WindowsPlatformLauncher;
import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacketProcessor;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SdWanNodeLauncher {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("t", "type", true, "type");
        options.addOption("a", "action", true, "action");
        options.addOption("n", "name", true, "name");
        options.addOption("c", "config", true, "config");
        options.addOption("log", "logFile", true, "logFile");
        options.addOption("debug", "debug", false, "debug");
        options.addOption("trace", "trace", false, "trace");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        Logger logger;
        if (cmd.hasOption("log")) {
            String logFile = cmd.getOptionValue("log");
            logger = new LoggerSystem().init(logFile);
        } else {
            logger = new LoggerSystem().initUserDir();
        }
        try {
            if (cmd.hasOption("debug")) {
                startTunSdWanNode(logger);
                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            } else if (PlatformDependent.isWindows()) {
                WindowsPlatformLauncher.startup(cmd);
            } else {
                startTunSdWanNode(logger);
                CountDownLatch latch = new CountDownLatch(1);
                latch.await();
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static TunSdWanNode startTunSdWanNode(Logger logger) throws Exception {
        logger.info("startTunSdWanNode");
        SdWanNodeConfig config = new ConfigSystem().initUserDir();
        TunSdWanNode tunSdWanNode = new TunSdWanNode(config);
        tunSdWanNode.addIpLayerPacketProcessor(new IpLayerPacketProcessor() {
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
        tunSdWanNode.start();
        return tunSdWanNode;
    }
}
