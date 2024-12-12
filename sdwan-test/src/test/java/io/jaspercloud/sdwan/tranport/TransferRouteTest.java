package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransferRouteTest {

    @Test
    public void test() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.11");
                put("x2:x:x:x:x:x", "10.5.0.12");
            }
        };
        List<ControllerServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            return ControllerServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
        }).collect(Collectors.toList());
        List<ControllerServerConfig.Route> routeList = new ArrayList<>();
        routeList.add(ControllerServerConfig.Route.builder()
                .destination("172.168.1.0/24")
                .nexthop(Arrays.asList("10.5.0.12"))
                .build());
        ControllerServerConfig config = ControllerServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", ControllerServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build();
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        controllerServer.start();
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.start();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost("127.0.0.1")
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.start();
        SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
        nodeConfig1.setControllerServer("127.0.0.1:1800");
        nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig1.setP2pPort(1001);
        TestSdWanNode sdWanNode1 = new TestSdWanNode(nodeConfig1) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.start();
        SdWanNodeConfig nodeConfig2 = new SdWanNodeConfig();
        nodeConfig2.setControllerServer("127.0.0.1:1800");
        nodeConfig2.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig2.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig2.setP2pPort(1002);
        TestSdWanNode sdWanNode2 = new TestSdWanNode(nodeConfig2) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.start();
        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                .setSrcIP("192.168.1.2")
                .setDstIP("172.168.1.2")
                .setPayload(ByteString.copyFrom("hello1".getBytes()))
                .build());
        Thread.sleep(5 * 1000);
        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                .setSrcIP("192.168.1.2")
                .setDstIP("10.5.0.12")
                .setPayload(ByteString.copyFrom("hello2".getBytes()))
                .build());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    @Test
    public void onlyRelayTransport() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.11");
                put("x2:x:x:x:x:x", "10.5.0.12");
            }
        };
        List<ControllerServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            return ControllerServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
        }).collect(Collectors.toList());
        List<ControllerServerConfig.Route> routeList = new ArrayList<>();
        routeList.add(ControllerServerConfig.Route.builder()
                .destination("172.168.1.0/24")
                .nexthop(Arrays.asList("10.5.0.12"))
                .build());
        ControllerServerConfig config = ControllerServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", ControllerServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build();
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        controllerServer.start();
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.start();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost("127.0.0.1")
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.start();
        SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
        nodeConfig1.setControllerServer("127.0.0.1:1800");
        nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig1.setP2pPort(1001);
        TestSdWanNode sdWanNode1 = new TestSdWanNode(nodeConfig1) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.start();
        SdWanNodeConfig nodeConfig2 = new SdWanNodeConfig();
        nodeConfig2.setControllerServer("127.0.0.1:1800");
        nodeConfig2.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig2.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig2.setP2pPort(1002);
        TestSdWanNode sdWanNode2 = new TestSdWanNode(nodeConfig2) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.start();
        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                .setSrcIP("192.168.1.2")
                .setDstIP("172.168.1.2")
                .setPayload(ByteString.copyFrom("hello1".getBytes()))
                .build());
        Thread.sleep(5 * 1000);
        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                .setSrcIP("192.168.1.2")
                .setDstIP("10.5.0.12")
                .setPayload(ByteString.copyFrom("hello2".getBytes()))
                .build());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

    @Test
    public void reply() throws Exception {
        Lifecycle main = new Lifecycle() {

            private TestSdWanNode sdWanNode1;
            private TestSdWanNode sdWanNode2;

            @Override
            public void start() throws Exception {
                Map<String, String> fixedVipMap = new HashMap<String, String>() {
                    {
                        put("x1:x:x:x:x:x", "10.5.0.11");
                        put("x2:x:x:x:x:x", "10.5.0.12");
                    }
                };
                List<ControllerServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
                    return ControllerServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
                }).collect(Collectors.toList());
                List<ControllerServerConfig.Route> routeList = new ArrayList<>();
                routeList.add(ControllerServerConfig.Route.builder()
                        .destination("172.168.1.0/24")
                        .nexthop(Arrays.asList("10.5.0.12"))
                        .build());
                ControllerServerConfig config = ControllerServerConfig.builder()
                        .port(1800)
                        .heartTimeout(30 * 1000)
                        .tenantConfig(Collections.singletonMap("tenant1", ControllerServerConfig.TenantConfig.builder()
                                .vipCidr("10.5.0.0/24")
                                .fixedVipList(fixVipList)
                                .routeList(routeList)
                                .build()))
                        .build();
                LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
                ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
                controllerServer.start();
                RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                        .bindPort(2478)
                        .heartTimeout(15000)
                        .build(), () -> new ChannelInboundHandlerAdapter());
                relayServer.start();
                StunServer stunServer = new StunServer(StunServerConfig.builder()
                        .bindHost("127.0.0.1")
                        .bindPort(3478)
                        .build(), () -> new ChannelInboundHandlerAdapter());
                stunServer.start();
                SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
                nodeConfig1.setControllerServer("127.0.0.1:1800");
                nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
                nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
                sdWanNode1 = new TestSdWanNode(nodeConfig1) {
                    @Override
                    protected String processMacAddress(String hardwareAddress) {
                        return "x1:x:x:x:x:x";
                    }
                };
                sdWanNode1.start();
                SdWanNodeConfig nodeConfig2 = new SdWanNodeConfig();
                nodeConfig2.setControllerServer("127.0.0.1:1800");
                nodeConfig2.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
                nodeConfig2.setStunServerList(Arrays.asList("127.0.0.1:3478"));
                sdWanNode2 = new TestSdWanNode(nodeConfig2) {
                    @Override
                    protected String processMacAddress(String hardwareAddress) {
                        return "x2:x:x:x:x:x";
                    }

                    @Override
                    protected void onData(IpLayerPacket packet) {
                        SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.newBuilder()
                                .setSrcIP(packet.getDstIP())
                                .setDstIP(packet.getSrcIP())
                                .setPayload(ByteString.copyFrom("hello reply".getBytes()))
                                .build();
                        sdWanNode2.sendIpPacket(ipPacket);
                    }
                };
                sdWanNode2.start();
                System.out.println("test started");
                sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                        .setSrcIP("10.5.0.11")
                        .setDstIP("10.5.0.12")
                        .setPayload(ByteString.copyFrom("hello".getBytes()))
                        .build());
            }

            @Override
            public void stop() throws Exception {

            }
        };
        main.start();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
