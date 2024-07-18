package io.jaspercloud.sdwan.tranport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.impl.StaticLoggerBinder;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ChaosServerTest {

    public static void main(String[] args) throws Exception {
        applyLog();
        applySdWanServer(60 * 1000, 120 * 1000, 5 * 1000);
        applyRelayServer(60 * 1000, 120 * 1000, 5 * 1000);
        applyStunServer(60 * 1000, 120 * 1000, 5 * 1000);
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        List<Logger> loggerList = loggerContext.getLoggerList();
        loggerList.forEach(log -> {
            if (log.getName().startsWith("io.jaspercloud.sdwan")) {
                log.setLevel(Level.DEBUG);
            }
        });
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    private static void applyNode(String tun, String mac, long min, long max, long interval) {
        new Thread(() -> {
            while (true) {
                try {
                    String address = InetAddress.getLocalHost().getHostAddress();
                    TunSdWanNode sdWanNode = new TunSdWanNode(SdWanNodeConfig.builder()
                            .controllerServer(address + ":1800")
                            .relayServer(address + ":2478")
                            .stunServer(address + ":3478")
                            .connectTimeout(3 * 1000)
                            .heartTime(15 * 1000)
                            .p2pHeartTime(10 * 1000)
                            .tunName(tun)
                            .mtu(1440)
                            .build()) {
                        @Override
                        protected String processMacAddress(String hardwareAddress) {
                            return mac;
                        }
                    };
                    sdWanNode.start();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    sdWanNode.stop();
                    Thread.sleep(interval);
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private static void applyStunServer(long min, long max, long interval) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            while (true) {
                try {
                    String address = InetAddress.getLocalHost().getHostAddress();
                    StunServer stunServer = new StunServer(StunServerConfig.builder()
                            .bindHost(address)
                            .bindPort(3478)
                            .build(), () -> new ChannelInboundHandlerAdapter());
                    stunServer.start();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    stunServer.stop();
                    Thread.sleep(interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void applyRelayServer(long min, long max, long interval) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            while (true) {
                try {
                    RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                            .bindPort(2478)
                            .heartTimeout(15000)
                            .build(), () -> new ChannelInboundHandlerAdapter());
                    relayServer.start();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    relayServer.stop();
                    Thread.sleep(interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        countDownLatch.await();
    }

    private static void applySdWanServer(long min, long max, long interval) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        new Thread(() -> {
            while (true) {
                try {
                    Map<String, String> fixedVipMap = new HashMap<String, String>() {
                        {
                            put("x1:x:x:x:x:x", "10.5.0.11");
                            put("x2:x:x:x:x:x", "10.5.0.12");
                        }
                    };
                    List<SdWanServerConfig.Route> routeList = new ArrayList<>();
                    SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                            .port(1800)
                            .heartTimeout(30 * 1000)
                            .vipCidr("10.5.0.0/24")
                            .fixedVipList(Collections.emptyList())
                            .routeList(routeList)
                            .build(), () -> new ChannelInboundHandlerAdapter());
                    sdWanServer.start();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    sdWanServer.stop();
                    Thread.sleep(interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void applyLog() {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        Logger logger = loggerContext.getLogger("ROOT");
        logger.setLevel(Level.INFO);
    }
}
