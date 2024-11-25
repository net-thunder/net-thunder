package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.TunSdWanNode;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class NodeInfoTest {

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
        SdWanNodeConfig nodeConfig = new SdWanNodeConfig();
        nodeConfig.setTunName("tun1");
        TunSdWanNode sdWanNode1 = new TunSdWanNode(nodeConfig) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }
        };
        sdWanNode1.start();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}
