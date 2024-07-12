package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.SdWanNodeConfig;
import io.jaspercloud.sdwan.TunSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/9
 */
public class TunTransferTest {

    @Test
    public void test() throws Exception {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
        String address = InetAddress.getLocalHost().getHostAddress();
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.1");
                put("x2:x:x:x:x:x", "10.5.0.2");
            }
        };
        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
        routeList.add(SdWanServerConfig.Route.builder()
                .destination("172.168.1.0/24")
                .nexthop(Arrays.asList("10.5.0.2"))
                .build());
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .vipCidr("10.5.0.0/24")
                .fixedVipMap(fixedVipMap)
                .routeList(routeList)
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.afterPropertiesSet();
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.afterPropertiesSet();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost(address)
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.afterPropertiesSet();
        TunSdWanNode sdWanNode1 = new TunSdWanNode(SdWanNodeConfig.builder()
                .controllerServer(address + ":1800")
                .relayServer(address + ":2478")
                .stunServer("127.0.0.1:3478")
                .p2pPort(1001)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .tunName("tun1")
                .mtu(1440)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.afterPropertiesSet();
        TunSdWanNode sdWanNode2 = new TunSdWanNode(SdWanNodeConfig.builder()
                .controllerServer(address + ":1800")
                .relayServer(address + ":2478")
                .stunServer("127.0.0.1:3478")
                .p2pPort(1002)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .tunName("tun2")
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.afterPropertiesSet();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
