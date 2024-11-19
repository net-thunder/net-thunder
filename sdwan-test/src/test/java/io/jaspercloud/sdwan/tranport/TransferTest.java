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
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransferTest {

    @Test
    public void test() throws Exception {
        String address = InetAddress.getLocalHost().getHostAddress();
        List<String> stunList = new ArrayList<>();
        List<String> relayList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            StunServer stunServer = new StunServer(StunServerConfig.builder()
                    .bindHost(address)
                    .bindPort(3000 + i)
                    .build(), () -> new ChannelInboundHandlerAdapter());
            stunServer.start();
            stunList.add(String.format("localhost:%s", 3000 + i));
            RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                    .bindPort(4000 + i)
                    .heartTimeout(15 * 1000)
                    .build(), () -> new ChannelInboundHandlerAdapter());
            relayServer.start();
            relayList.add(String.format("localhost:%s", 4000 + i));
        }
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
                .tenantConfig(Collections.singletonMap("default", SdWanServerConfig.TenantConfig.builder()
                        .stunServerList(stunList)
                        .relayServerList(relayList)
                        .vipCidr("10.5.0.0/24")
                        .fixedVipList(fixVipList)
                        .routeList(routeList)
                        .build()))
                .build();
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        SdWanServer sdWanServer = new SdWanServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
                .onlyRelayTransport(false)
                .controllerServer(address + ":1800")
                .tunName("tun1")
                .p2pPort(1001)
                .tenantId("default")
                .connectTimeout(5 * 1000)
                .heartTime(5 * 1000)
                .electionTimeout(3000)
                .iceHeartTime(1000)
                .iceTimeout(5 * 1000)
                .p2pHeartTime(1000)
                .p2pTimeout(5 * 1000)
                .netMesh(false)
                .autoReconnect(true)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.start();
        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
                .onlyRelayTransport(false)
                .controllerServer(address + ":1800")
                .tunName("tun2")
                .p2pPort(1002)
                .tenantId("default")
                .connectTimeout(5 * 1000)
                .heartTime(5 * 1000)
                .electionTimeout(3000)
                .iceHeartTime(1000)
                .iceTimeout(5 * 1000)
                .p2pHeartTime(1000)
                .p2pTimeout(5 * 1000)
                .netMesh(false)
                .autoReconnect(true)
                .build()) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }
        };
        sdWanNode2.start();
        while (true) {
            sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                    .setSrcIP("10.5.0.11")
                    .setDstIP("10.5.0.12")
                    .setPayload(ByteString.copyFrom("hello".getBytes()))
                    .build());
            Thread.sleep(3 * 1000);
        }
    }
}
