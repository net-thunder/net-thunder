package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.node.VirtualRouter;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleStrategyEnum;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.jaspercloud.sdwan.tun.IcmpPacket;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.*;
import java.util.stream.Collectors;

public class VirtualRouterTest {

    @Test
    public void test() throws Exception {
        startSDServer();
        startStunServer();
        startRelayServer();
        SdWanNodeConfig config = new SdWanNodeConfig();
        config.setShowICELog(false);
        config.setShowElectionLog(false);
        config.setShowRouteRuleLog(false);
        config.setControllerServer("localhost:1800");
        config.setStunServerList(Arrays.asList("localhost:3478"));
        config.setRelayServerList(Arrays.asList("localhost:2478"));
        VirtualRouter router1 = new VirtualRouter(config) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x1:x:x:x:x:x";
            }

            @Override
            protected void onData(VirtualRouter router, IpLayerPacket packet) {
                System.out.println(String.format("onResponse: src=%s, dst=%s", packet.getSrcIP(), packet.getDstIP()));
            }

            @Override
            protected void onSdWanClientInactive() {
                super.onSdWanClientInactive();
            }
        };
        VirtualRouter router2 = new VirtualRouter(config) {
            @Override
            protected String processMacAddress(String hardwareAddress) {
                return "x2:x:x:x:x:x";
            }

            @Override
            protected void onData(VirtualRouter router, IpLayerPacket packet) {
                System.out.println(String.format("onRequest: src=%s, dst=%s", packet.getSrcIP(), packet.getDstIP()));
                IcmpPacket echo = echo(System.nanoTime());
                Ipv4Packet request = ipv4Packet(echo, packet.getDstIP(), packet.getSrcIP());
                router.send(new IpLayerPacket(request.encode(false, false)));
            }

            @Override
            protected void onUpdateRouteList(List<SDWanProtos.Route> routeList) {
                super.onUpdateRouteList(routeList);
            }

            @Override
            protected void onSdWanClientInactive() {
                super.onSdWanClientInactive();
            }
        };
        router1.start();
        router2.start();
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        while (true) {
//                IcmpPacket echo = echo(System.nanoTime());
//                Ipv4Packet request = ipv4Packet(echo, "10.5.0.11", "10.5.0.12");
//                router1.send(new IpLayerPacket(request.encode(false, false)));
//            Thread.sleep(1000);
//        }
//        countDownLatch.await();

//        while (true) {
//            IcmpPacket echo = echo(System.nanoTime());
//            Ipv4Packet request = ipv4Packet(echo, "10.5.0.11", "192.168.1.5");
//            router1.send(new IpLayerPacket(request.encode(false, false)));
//            Thread.sleep(1000);
//        }

        while (true) {
            IcmpPacket echo = echo(System.nanoTime());
            Ipv4Packet request = ipv4Packet(echo, "10.5.0.11", "172.168.1.5");
            router1.send(new IpLayerPacket(request.encode(false, false)));
            Thread.sleep(1000);
        }
    }

    public IcmpPacket echo(long time) {
        IcmpPacket icmpPacket = new IcmpPacket();
        icmpPacket.setType(IcmpPacket.Echo);
        icmpPacket.setCode((byte) 0);
        icmpPacket.setIdentifier(RandomUtils.nextInt(1000, 5000));
        icmpPacket.setSequence(RandomUtils.nextInt(1000, 5000));
        icmpPacket.setTimestamp(time);
        ByteBuf payload = ByteBufUtil.create();
        payload.writeLong(time);
        icmpPacket.setPayload(payload);
        return icmpPacket;
    }

    public Ipv4Packet ipv4Packet(IcmpPacket icmpPacket, String srcIP, String dstIP) {
        ByteBuf icmpEncode = icmpPacket.encode();
        Ipv4Packet ipv4Packet = new Ipv4Packet();
        ipv4Packet.setVersion((byte) 4);
        ipv4Packet.setDiffServices((byte) 0);
        ipv4Packet.setIdentifier(RandomUtils.nextInt(1000, 5000));
        ipv4Packet.setFlags((byte) 0);
        ipv4Packet.setLiveTime((short) 128);
        ipv4Packet.setProtocol(Ipv4Packet.Icmp);
        ipv4Packet.setSrcIP(srcIP);
        ipv4Packet.setDstIP(dstIP);
        ipv4Packet.setPayload(icmpEncode);
        return ipv4Packet;
    }

    private static void startStunServer() throws Exception {
        String address = InetAddress.getLocalHost().getHostAddress();
        StunServer stunServer = new StunServer(StunServerConfig.builder()
                .bindHost(address)
                .bindPort(3478)
                .build(), () -> new ChannelInboundHandlerAdapter());
        stunServer.start();
    }

    private void startRelayServer() throws Exception {
        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
                .bindPort(2478)
                .heartTimeout(15000)
                .build(), () -> new ChannelInboundHandlerAdapter());
        relayServer.start();
    }

    private void startSDServer() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("x1:x:x:x:x:x", "10.5.0.11");
                put("x2:x:x:x:x:x", "10.5.0.12");
            }
        };
        List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            SdWanServerConfig.FixVip fixVip = new SdWanServerConfig.FixVip();
            fixVip.setMac(e.getKey());
            fixVip.setVip(e.getValue());
            return fixVip;
        }).collect(Collectors.toList());
        SdWanServerConfig.TenantConfig tenantConfig = new SdWanServerConfig.TenantConfig();
        tenantConfig.setVipCidr("10.5.0.0/24");
        tenantConfig.setFixedVipList(fixVipList);
        tenantConfig.setRouteList(Arrays.asList(
                new SdWanServerConfig.Route() {
                    {
                        setDestination("192.168.1.0/24");
                        setNexthop(Arrays.asList("10.5.0.12"));
                    }
                }
        ));
        tenantConfig.setVnatList(Arrays.asList(
                new SdWanServerConfig.VNAT() {
                    {
                        setSrc("172.168.1.0/24");
                        setDst("192.168.1.0/24");
                        setVipList(Arrays.asList("10.5.0.12"));
                    }
                }
        ));
        tenantConfig.setRouteRuleList(Arrays.asList(
                new SdWanServerConfig.RouteRule() {
                    {
                        setStrategy(RouteRuleStrategyEnum.Allow);
                        setDirection(RouteRuleDirectionEnum.All);
                        setRuleList(Arrays.asList("0.0.0.0/0"));
                        setLevel(1);
                    }
                }
        ));
        tenantConfig.setStunServerList(Arrays.asList("localhost:3478"));
        tenantConfig.setRelayServerList(Arrays.asList("localhost:2478"));
        Map<String, SdWanServerConfig.TenantConfig> tenantConfigMap = Collections.singletonMap("default", tenantConfig);
        SdWanServerConfig config = new SdWanServerConfig();
        config.setTenantConfig(tenantConfigMap);
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        SdWanServer sdWanServer = new SdWanServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        sdWanServer.start();
    }
}
