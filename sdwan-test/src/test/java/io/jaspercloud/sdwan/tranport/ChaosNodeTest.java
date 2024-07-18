package io.jaspercloud.sdwan.tranport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.impl.StaticLoggerBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ChaosNodeTest {

    public static void main(String[] args) throws Exception {
        applyLog();
        applyNode("tun1", "x1:x:x:x:x:x", 60 * 1000, 120 * 1000, 5 * 1000);
        applyNode("tun2", "x2:x:x:x:x:x", 15 * 1000, 30 * 1000, 5 * 1000);
        LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        List<Logger> loggerList = loggerContext.getLoggerList();
        loggerList.forEach(log -> {
            if (log.getName().startsWith("io.jaspercloud.sdwan")) {
                log.setLevel(Level.DEBUG);
            }
        });
        Process process = Runtime.getRuntime().exec("ping -S 10.5.0.11 10.5.0.12 -n 1000");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
        String line;
        while (null != (line = reader.readLine())) {
            System.out.println("process: " + line);
        }
        System.out.println();
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
                    sdWanNode.afterPropertiesSet();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    sdWanNode.destroy();
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
                    stunServer.afterPropertiesSet();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    stunServer.destroy();
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
                    relayServer.afterPropertiesSet();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    relayServer.destroy();
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
//                    routeList.add(SdWanServerConfig.Route.builder()
//                            .destination("172.168.1.0/24")
//                            .nexthop(Arrays.asList("10.5.0.12"))
//                            .build());
                    SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                            .port(1800)
                            .heartTimeout(30 * 1000)
                            .vipCidr("10.5.0.0/24")
                            .fixedVipMap(fixedVipMap)
                            .routeList(routeList)
                            .build(), () -> new ChannelInboundHandlerAdapter());
                    sdWanServer.afterPropertiesSet();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    sdWanServer.destroy();
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
