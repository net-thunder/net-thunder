package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.TunSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author jasper
 * @create 2024/7/9
 */
public class SdWanNodePushTest {

    @Test
    public void pushRoute() throws Exception {
        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
        routeList.add(SdWanServerConfig.Route.builder()
                .destination("172.168.1.0/24")
                .nexthop(Arrays.asList("10.5.0.2"))
                .build());
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .vipCidr("10.5.0.0/24")
                .routeList(routeList)
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.afterPropertiesSet();
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.afterPropertiesSet();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost("127.0.0.1")
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.afterPropertiesSet();
        TunSdWanNode sdWanNode = new TunSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .tunName("net-thunder")
                .build()) {
        };
        sdWanNode.afterPropertiesSet();
        System.out.println("started");
        {
            SDWanProtos.RouteList routeListData = SDWanProtos.RouteList.newBuilder()
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.2.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.3.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .build();
            sdWanServer.push(SDWanProtos.MessageTypeCode.RouteListType, routeListData);
            System.out.println("push RouteList1");
        }
        Thread.sleep(5 * 1000);
        {
            SDWanProtos.RouteList routeListData = SDWanProtos.RouteList.newBuilder()
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.4.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.5.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .build();
            sdWanServer.push(SDWanProtos.MessageTypeCode.RouteListType, routeListData);
            System.out.println("push RouteList1");
        }
        Thread.sleep(5 * 1000);
        {
            SDWanProtos.RouteList routeListData = SDWanProtos.RouteList.newBuilder()
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.6.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.7.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.8.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .addRoute(SDWanProtos.Route.newBuilder()
                            .setDestination("172.168.9.0/24")
                            .addAllNexthop(Arrays.asList("10.5.0.2"))
                            .build())
                    .build();
            sdWanServer.push(SDWanProtos.MessageTypeCode.RouteListType, routeListData);
            System.out.println("push RouteList1");
        }
        Thread.sleep(5 * 1000);
        System.out.println();
    }

    @Test
    public void pushNodeStatus() throws Exception {
        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
        routeList.add(SdWanServerConfig.Route.builder()
                .destination("172.168.1.0/24")
                .nexthop(Arrays.asList("10.5.0.2"))
                .build());
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .vipCidr("10.5.0.0/24")
                .routeList(routeList)
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.afterPropertiesSet();
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.afterPropertiesSet();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost("127.0.0.1")
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.afterPropertiesSet();
        TunSdWanNode sdWanNode = new TunSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .tunName("net-thunder")
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode.afterPropertiesSet();
        System.out.println("started");
        for (int i = 0; i < 5; i++) {
            TunSdWanNode testNode = new TunSdWanNode(SdWanNodeConfig.builder()
                    .controllerServer("127.0.0.1:1800")
                    .relayServer("127.0.0.1:2478")
                    .stunServer("127.0.0.1:3478")
                    .heartTime(15 * 1000)
                    .p2pHeartTime(10 * 1000)
                    .tunName("tun1")
                    .build()) {
                @Override
                protected String processMacAddress(String hardwareAddress) {
                    return "x2:x:x:x:x:x";
                }
            };
            testNode.afterPropertiesSet();
            testNode.destroy();
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
