package io.jaspercloud.sdwan.tranport;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class ChaosTest {

    public static void main(String[] args) throws Exception {
        applyLog();
        List<String> stunServerList = applyStunServer(5, 15 * 1000, 30 * 1000, 5 * 1000);
        List<String> relayServerList = applyRelayServer(5, 15 * 1000, 30 * 1000, 8 * 1000);
        applySdWanServer(stunServerList, relayServerList, 30 * 1000, 60 * 1000, 3 * 1000);
        applyNode("tun1", "x1:x:x:x:x:x", 60 * 1000, 120 * 1000, 15 * 1000);
        applyNode("tun2", "x2:x:x:x:x:x", 15 * 1000, 30 * 1000, 15 * 1000);
//        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        List<Logger> loggerList = loggerContext.getLoggerList();
//        loggerList.forEach(log -> {
//            if (log.getName().startsWith("io.jaspercloud.sdwan")) {
//                log.setLevel(Level.DEBUG);
//            }
//        });
        // ping -S 10.5.0.11 10.5.0.12 -n 1000
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    private static void applyNode(String tun, String mac, long min, long max, long interval) {
        new Thread(() -> {
            while (true) {
                try {
                    SdWanNodeConfig config = new SdWanNodeConfig();
                    config.setControllerServer("127.0.0.1:1800");
                    config.setTunName(tun);
                    TunSdWanNode sdWanNode = new TunSdWanNode(config) {
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
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static List<String> applyStunServer(int count, long min, long max, long interval) throws Exception {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            int port = 2000 + i;
            list.add(String.format("127.0.0.1:%s", port));
            CountDownLatch countDownLatch = new CountDownLatch(1);
            new Thread(() -> {
                while (true) {
                    try {
                        StunServerConfig config = new StunServerConfig();
                        config.setBindPort(port);
                        StunServer stunServer = new StunServer(config, () -> new ChannelInboundHandlerAdapter());
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
        return list;
    }

    private static List<String> applyRelayServer(int count, long min, long max, long interval) throws Exception {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            int port = 3000 + i;
            list.add(String.format("127.0.0.1:%s", port));
            CountDownLatch countDownLatch = new CountDownLatch(1);
            new Thread(() -> {
                while (true) {
                    try {
                        RelayServerConfig config = new RelayServerConfig();
                        config.setBindPort(port);
                        RelayServer relayServer = new RelayServer(config, () -> new ChannelInboundHandlerAdapter());
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
        return list;
    }

    private static void applySdWanServer(List<String> stunServerList,
                                         List<String> relayServerList,
                                         long min, long max, long interval) throws Exception {
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
                    List<ControllerServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
                        ControllerServerConfig.FixVip fixVip = new ControllerServerConfig.FixVip();
                        fixVip.setMac(e.getKey());
                        fixVip.setVip(e.getValue());
                        return fixVip;
                    }).collect(Collectors.toList());
                    ControllerServerConfig.TenantConfig tenantConfig = ControllerServerConfig.TenantConfig.builder()
                            .vipCidr("10.5.0.0/24")
                            .stunServerList(stunServerList)
                            .relayServerList(relayServerList)
                            .fixedVipList(fixVipList)
                            .routeList(Collections.emptyList())
                            .vnatList(Collections.emptyList())
                            .build();
                    ControllerServerConfig config = new ControllerServerConfig();
                    config.setTenantConfig(Collections.singletonMap("default", tenantConfig));
                    LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
                    ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
                    controllerServer.start();
                    countDownLatch.countDown();
                    Thread.sleep(RandomUtils.nextLong(min, max));
                    controllerServer.stop();
                    Thread.sleep(interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static void applyLog() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger("ROOT");
        logger.setLevel(Level.INFO);
    }
}
