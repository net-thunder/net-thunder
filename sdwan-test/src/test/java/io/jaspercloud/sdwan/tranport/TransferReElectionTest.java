package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.stun.NatAddress;
import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransferReElectionTest {

    @Test
    public void relayTrue() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.11");
                put("x2:x:x:x:x:x", "10.5.0.12");
            }
        };
        List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            return SdWanServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
        }).collect(Collectors.toList());
        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
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
        String localAddress = InetAddress.getLocalHost().getHostAddress();
        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1001)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }

            @Override
            protected NatAddress processNatAddress(NatAddress mappingAddress) {
                mappingAddress.setMappingAddress(new InetSocketAddress("192.168.1.0", 1000));
                return mappingAddress;
            }
        };
        sdWanNode1.start();
        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1002)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }

            @Override
            protected NatAddress processNatAddress(NatAddress mappingAddress) {
                mappingAddress.setMappingAddress(new InetSocketAddress("192.168.1.0", 1000));
                return mappingAddress;
            }
        };
        sdWanNode2.start();
        int i = 1;
        while (true) {
            sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                    .setSrcIP("192.168.1.2")
                    .setDstIP("10.5.0.12")
                    .setPayload(ByteString.copyFrom(("hello" + i++).getBytes()))
                    .build());
            Thread.sleep(3000);
        }
    }

    @Test
    public void relayFalse() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.11");
                put("x2:x:x:x:x:x", "10.5.0.12");
            }
        };
        List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            return SdWanServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
        }).collect(Collectors.toList());
        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
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
        String localAddress = InetAddress.getLocalHost().getHostAddress();
        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1001)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.start();
        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1002)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.start();
        int i = 1;
        while (true) {
            sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                    .setSrcIP("192.168.1.2")
                    .setDstIP("10.5.0.2")
                    .setPayload(ByteString.copyFrom(("hello" + i++).getBytes()))
                    .build());
            Thread.sleep(3000);
        }
    }

    @Test
    public void relayRandom() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.11");
                put("x2:x:x:x:x:x", "10.5.0.12");
            }
        };
        List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            return SdWanServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
        }).collect(Collectors.toList());
        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
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
        String localAddress = InetAddress.getLocalHost().getHostAddress();
        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1001)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.start();
        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .relayServer("127.0.0.1:2478")
                .stunServer("127.0.0.1:3478")
                .localAddress(localAddress)
                .p2pPort(1002)
                .heartTime(15 * 1000)
                .p2pHeartTime(10 * 1000)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.start();
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    sdWanNode1.getIceClient().getP2pTransportManager().clear();
                }, 0, 10 * 1000, TimeUnit.MILLISECONDS);
        int i = 1;
        while (true) {
            sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                    .setSrcIP("192.168.1.2")
                    .setDstIP("10.5.0.12")
                    .setPayload(ByteString.copyFrom(("hello" + i++).getBytes()))
                    .build());
            Thread.sleep(3000);
        }
    }
}
