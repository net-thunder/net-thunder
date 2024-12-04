package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
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
        Map<String, SdWanServerConfig.TenantConfig> tenantConfigMap = Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
                .vipCidr("10.5.0.0/24")
                .fixedVipList(fixVipList)
                .routeList(routeList)
                .build());
        SdWanServerConfig config = new SdWanServerConfig();
        config.setTenantConfig(tenantConfigMap);
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        SdWanServer sdWanServer = new SdWanServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
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
        SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
        nodeConfig1.setControllerServer("127.0.0.1:1800");
        nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig1.setLocalAddress(localAddress);
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
        nodeConfig2.setLocalAddress(localAddress);
        nodeConfig2.setP2pPort(1002);
        TestSdWanNode sdWanNode2 = new TestSdWanNode(nodeConfig2) {
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
        SdWanServerConfig config = SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build();
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        SdWanServer sdWanServer = new SdWanServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
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
        SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
        nodeConfig1.setControllerServer("127.0.0.1:1800");
        nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig1.setLocalAddress(localAddress);
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
        nodeConfig2.setLocalAddress(localAddress);
        nodeConfig2.setP2pPort(1002);
        TestSdWanNode sdWanNode2 = new TestSdWanNode(nodeConfig2) {
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
        SdWanServerConfig config = SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build();
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        SdWanServer sdWanServer = new SdWanServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
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
        SdWanNodeConfig nodeConfig1 = new SdWanNodeConfig();
        nodeConfig1.setControllerServer("127.0.0.1:1800");
        nodeConfig1.setRelayServerList(Arrays.asList("127.0.0.1:2478"));
        nodeConfig1.setStunServerList(Arrays.asList("127.0.0.1:3478"));
        nodeConfig1.setLocalAddress(localAddress);
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
        nodeConfig2.setLocalAddress(localAddress);
        nodeConfig2.setP2pPort(1002);
        TestSdWanNode sdWanNode2 = new TestSdWanNode(nodeConfig2) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.start();
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    sdWanNode1.getVirtualRouter().getIceClient().getP2pTransportManager().clear();
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
