package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ByteString;
import io.jaspercloud.sdwan.support.SdWanNodeConfig;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class TransferTest {

    @Test
    public void test() throws Exception {
        String address = InetAddress.getLocalHost().getHostAddress();
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
                .vipCidr("10.5.0.0/24")
                .fixedVipList(fixVipList)
                .routeList(routeList)
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.start();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost(address)
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.start();
        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
                .controllerServer(address + ":1800")
                .relayServer(address + ":2478")
                .stunServer("127.0.0.1:3478")
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
                .controllerServer(address + ":1800")
                .relayServer(address + ":2478")
                .stunServer("127.0.0.1:3478")
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
        while (true) {
            sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
                    .setSrcIP("10.5.0.1")
                    .setDstIP("10.5.0.2")
                    .setPayload(ByteString.copyFrom("hello".getBytes()))
                    .build());
            Thread.sleep(1000);
        }
    }
}
