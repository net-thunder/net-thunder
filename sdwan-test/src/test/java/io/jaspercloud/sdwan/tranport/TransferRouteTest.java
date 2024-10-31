//package io.jaspercloud.sdwan.tranport;
//
//import com.google.protobuf.ByteString;
//import io.jaspercloud.sdwan.core.proto.SDWanProtos;
//import io.jaspercloud.sdwan.stun.*;
//import io.jaspercloud.sdwan.node.SdWanNodeConfig;
//import io.jaspercloud.sdwan.tranport.support.TestSdWanNode;
//import io.jaspercloud.sdwan.util.SocketAddressUtil;
//import io.netty.channel.ChannelHandler;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.channel.SimpleChannelInboundHandler;
//import org.junit.jupiter.api.Test;
//
//import java.net.InetSocketAddress;
//import java.util.*;
//import java.util.concurrent.CountDownLatch;
//import java.util.stream.Collectors;
//
///**
// * @author jasper
// * @create 2024/7/2
// */
//public class TransferRouteTest {
//
//    @Test
//    public void test() throws Exception {
//        Map<String, String> fixedVipMap = new HashMap<String, String>() {
//            {
//                put("x1:x:x:x:x:x", "10.5.0.11");
//                put("x2:x:x:x:x:x", "10.5.0.12");
//            }
//        };
//        List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
//            return SdWanServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
//        }).collect(Collectors.toList());
//        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
//        routeList.add(SdWanServerConfig.Route.builder()
//                .destination("172.168.1.0/24")
//                .nexthop(Arrays.asList("10.5.0.12"))
//                .build());
//        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
//                .port(1800)
//                .heartTimeout(30 * 1000)
//                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
//                        .vipCidr("10.5.0.0/24")
//                        .fixedVipList(fixVipList)
//                        .routeList(routeList)
//                        .build()))
//                .build(), () -> new ChannelInboundHandlerAdapter());
//        sdWanServer.start();
//        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
//                .bindPort(2478)
//                .heartTimeout(15000)
//                .build(), () -> new ChannelInboundHandlerAdapter());
//        relayServer.start();
//        StunServer stunServer = new StunServer(StunServerConfig.builder()
//                .bindHost("127.0.0.1")
//                .bindPort(3478)
//                .build(), () -> new ChannelInboundHandlerAdapter());
//        stunServer.start();
//        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
//                .controllerServer("127.0.0.1:1800")
//                .relayServer("127.0.0.1:2478")
//                .stunServer("127.0.0.1:3478")
//                .p2pPort(1001)
//                .heartTime(15 * 1000)
//                .p2pHeartTime(10 * 1000)
//                .build()) {
//            @Override
//            protected String processMacAddress(String hardwareAddress) {
//                return "x1:x:x:x:x:x";
//            }
//        };
//        sdWanNode1.start();
//        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
//                .controllerServer("127.0.0.1:1800")
//                .relayServer("127.0.0.1:2478")
//                .stunServer("127.0.0.1:3478")
//                .p2pPort(1002)
//                .heartTime(15 * 1000)
//                .p2pHeartTime(10 * 1000)
//                .build()) {
//            @Override
//            protected String processMacAddress(String hardwareAddress) {
//                return "x2:x:x:x:x:x";
//            }
//        };
//        sdWanNode2.start();
//        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
//                .setSrcIP("192.168.1.2")
//                .setDstIP("172.168.1.2")
//                .setPayload(ByteString.copyFrom("hello1".getBytes()))
//                .build());
//        Thread.sleep(5 * 1000);
//        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
//                .setSrcIP("192.168.1.2")
//                .setDstIP("10.5.0.12")
//                .setPayload(ByteString.copyFrom("hello2".getBytes()))
//                .build());
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//
//    @Test
//    public void onlyRelayTransport() throws Exception {
//        Map<String, String> fixedVipMap = new HashMap<String, String>() {
//            {
//                put("x1:x:x:x:x:x", "10.5.0.11");
//                put("x2:x:x:x:x:x", "10.5.0.12");
//            }
//        };
//        List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
//            return SdWanServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
//        }).collect(Collectors.toList());
//        List<SdWanServerConfig.Route> routeList = new ArrayList<>();
//        routeList.add(SdWanServerConfig.Route.builder()
//                .destination("172.168.1.0/24")
//                .nexthop(Arrays.asList("10.5.0.12"))
//                .build());
//        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
//                .port(1800)
//                .heartTimeout(30 * 1000)
//                .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
//                        .vipCidr("10.5.0.0/24")
//                        .fixedVipList(fixVipList)
//                        .routeList(routeList)
//                        .build()))
//                .build(), () -> new ChannelInboundHandlerAdapter());
//        sdWanServer.start();
//        RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
//                .bindPort(2478)
//                .heartTimeout(15000)
//                .build(), () -> new ChannelInboundHandlerAdapter());
//        relayServer.start();
//        StunServer stunServer = new StunServer(StunServerConfig.builder()
//                .bindHost("127.0.0.1")
//                .bindPort(3478)
//                .build(), () -> new ChannelInboundHandlerAdapter());
//        stunServer.start();
//        TestSdWanNode sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
//                .controllerServer("127.0.0.1:1800")
//                .relayServer("127.0.0.1:2478")
//                .stunServer("127.0.0.1:3478")
//                .p2pPort(1001)
//                .relayPort(2001)
//                .heartTime(15 * 1000)
//                .p2pHeartTime(10 * 1000)
//                .onlyRelayTransport(true)
//                .build()) {
//            @Override
//            protected String processMacAddress(String hardwareAddress) {
//                return "x1:x:x:x:x:x";
//            }
//        };
//        sdWanNode1.start();
//        TestSdWanNode sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
//                .controllerServer("127.0.0.1:1800")
//                .relayServer("127.0.0.1:2478")
//                .stunServer("127.0.0.1:3478")
//                .p2pPort(1002)
//                .relayPort(2002)
//                .heartTime(15 * 1000)
//                .p2pHeartTime(10 * 1000)
//                .onlyRelayTransport(true)
//                .build()) {
//            @Override
//            protected String processMacAddress(String hardwareAddress) {
//                return "x2:x:x:x:x:x";
//            }
//        };
//        sdWanNode2.start();
//        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
//                .setSrcIP("192.168.1.2")
//                .setDstIP("172.168.1.2")
//                .setPayload(ByteString.copyFrom("hello1".getBytes()))
//                .build());
//        Thread.sleep(5 * 1000);
//        sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
//                .setSrcIP("192.168.1.2")
//                .setDstIP("10.5.0.12")
//                .setPayload(ByteString.copyFrom("hello2".getBytes()))
//                .build());
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//
//    @Test
//    public void reply() throws Exception {
//        Lifecycle main = new Lifecycle() {
//
//            private TestSdWanNode sdWanNode1;
//            private TestSdWanNode sdWanNode2;
//
//            @Override
//            public void start() throws Exception {
//                Map<String, String> fixedVipMap = new HashMap<String, String>() {
//                    {
//                        put("x1:x:x:x:x:x", "10.5.0.11");
//                        put("x2:x:x:x:x:x", "10.5.0.12");
//                    }
//                };
//                List<SdWanServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
//                    return SdWanServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
//                }).collect(Collectors.toList());
//                List<SdWanServerConfig.Route> routeList = new ArrayList<>();
//                routeList.add(SdWanServerConfig.Route.builder()
//                        .destination("172.168.1.0/24")
//                        .nexthop(Arrays.asList("10.5.0.12"))
//                        .build());
//                SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
//                        .port(1800)
//                        .heartTimeout(30 * 1000)
//                        .tenantConfig(Collections.singletonMap("tenant1", SdWanServerConfig.TenantConfig.builder()
//                                .vipCidr("10.5.0.0/24")
//                                .fixedVipList(fixVipList)
//                                .routeList(routeList)
//                                .build()))
//                        .build(), () -> new ChannelInboundHandlerAdapter());
//                sdWanServer.start();
//                RelayServer relayServer = new RelayServer(RelayServerConfig.builder()
//                        .bindPort(2478)
//                        .heartTimeout(15000)
//                        .build(), () -> new ChannelInboundHandlerAdapter());
//                relayServer.start();
//                StunServer stunServer = new StunServer(StunServerConfig.builder()
//                        .bindHost("127.0.0.1")
//                        .bindPort(3478)
//                        .build(), () -> new ChannelInboundHandlerAdapter());
//                stunServer.start();
//                sdWanNode1 = new TestSdWanNode(SdWanNodeConfig.builder()
//                        .controllerServer("127.0.0.1:1800")
//                        .relayServer("127.0.0.1:2478")
//                        .stunServer("127.0.0.1:3478")
//                        .heartTime(15 * 1000)
//                        .p2pHeartTime(10 * 1000)
//                        .build()) {
//                    @Override
//                    protected String processMacAddress(String hardwareAddress) {
//                        return "x1:x:x:x:x:x";
//                    }
//                };
//                sdWanNode1.start();
//                sdWanNode2 = new TestSdWanNode(SdWanNodeConfig.builder()
//                        .controllerServer("127.0.0.1:1800")
//                        .relayServer("127.0.0.1:2478")
//                        .stunServer("127.0.0.1:3478")
//                        .heartTime(15 * 1000)
//                        .p2pHeartTime(10 * 1000)
//                        .build()) {
//                    @Override
//                    protected String processMacAddress(String hardwareAddress) {
//                        return "x2:x:x:x:x:x";
//                    }
//
//                    @Override
//                    protected ChannelHandler getProcessHandler() {
//                        return new SimpleChannelInboundHandler<StunPacket>() {
//                            @Override
//                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket msg) throws Exception {
//                                InetSocketAddress sender = msg.sender();
//                                StunMessage stunMessage = msg.content();
//                                StringAttr transferTypeAttr = stunMessage.getAttr(AttrType.TransferType);
//                                BytesAttr dataAttr = stunMessage.getAttr(AttrType.Data);
//                                byte[] data = dataAttr.getData();
//                                if (!MessageType.Transfer.equals(stunMessage.getMessageType())) {
//                                    return;
//                                }
//                                SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.parseFrom(data);
//                                System.out.println(String.format("recv transfer: type=%s, sender=%s, src=%s, dst=%s, data=%s",
//                                        transferTypeAttr.getData(), SocketAddressUtil.toAddress(sender),
//                                        ipPacket.getSrcIP(), ipPacket.getDstIP(), new String(ipPacket.getPayload().toByteArray())));
//                                ipPacket = ipPacket.toBuilder()
//                                        .setSrcIP(ipPacket.getDstIP())
//                                        .setDstIP(ipPacket.getSrcIP())
//                                        .setPayload(ByteString.copyFrom("hello reply".getBytes()))
//                                        .build();
//                                sdWanNode2.sendIpPacket(ipPacket);
//                            }
//                        };
//                    }
//                };
//                sdWanNode2.start();
//                System.out.println("test started");
//                sdWanNode1.sendIpPacket(SDWanProtos.IpPacket.newBuilder()
//                        .setSrcIP("10.5.0.11")
//                        .setDstIP("10.5.0.12")
//                        .setPayload(ByteString.copyFrom("hello".getBytes()))
//                        .build());
//            }
//
//            @Override
//            public void stop() throws Exception {
//
//            }
//        };
//        main.start();
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}
