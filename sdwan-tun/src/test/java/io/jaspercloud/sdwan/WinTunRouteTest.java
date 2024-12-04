//package io.jaspercloud.sdwan;
//
//import io.jaspercloud.sdwan.core.proto.SDWanProtos;
//import io.jaspercloud.sdwan.route.OSRouteTable;
//import io.jaspercloud.sdwan.route.OSWindowsRouteTable;
//import io.jaspercloud.sdwan.route.VirtualRouter;
//import io.jaspercloud.sdwan.tun.Ipv4Packet;
//import io.jaspercloud.sdwan.tun.TunAddress;
//import io.jaspercloud.sdwan.tun.TunChannel;
//import io.jaspercloud.sdwan.tun.TunChannelConfig;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.UnpooledByteBufAllocator;
//import io.netty.channel.*;
//
//import java.util.Arrays;
//import java.util.concurrent.CountDownLatch;
//
//public class WinTunRouteTest {
//
//    public static void main(String[] args) throws Exception {
//        DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();
//        Bootstrap bootstrap = new Bootstrap()
//                .group(eventLoopGroup)
//                .channel(TunChannel.class)
//                .option(TunChannelConfig.MTU, 1500)
//                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
//                .handler(new ChannelInitializer<Channel>() {
//                    @Override
//                    protected void initChannel(Channel ch) {
//                        ChannelPipeline pipeline = ch.pipeline();
//                        pipeline.addLast("TunEngine:readTun", new SimpleChannelInboundHandler<ByteBuf>() {
//                            @Override
//                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
//                                Ipv4Packet ipv4Packet = Ipv4Packet.decodeMark(msg);
//                                System.out.println(String.format("%s -> %s: protocol=%s",
//                                        ipv4Packet.getSrcIP(), ipv4Packet.getDstIP(), ipv4Packet.getProtocol()));
//                            }
//                        });
//                    }
//                });
//        String ip = "10.5.0.5";
//        ChannelFuture future = bootstrap.bind(new TunAddress("net-thunder", ip, 24));
//        TunChannel tunChannel = (TunChannel) future.syncUninterruptibly().channel();
//        VirtualRouter virtualRouter = new VirtualRouter();
//        virtualRouter.updateRoutes(Arrays.asList(
//                SDWanProtos.Route.newBuilder()
//                        .setDestination("172.168.1.0/24")
//                        .addAllNexthop(Arrays.asList("10.5.0.5"))
//                        .build(),
//                SDWanProtos.Route.newBuilder()
//                        .setDestination("172.168.2.0/24")
//                        .addAllNexthop(Arrays.asList("10.5.0.5"))
//                        .build(),
//                SDWanProtos.Route.newBuilder()
//                        .setDestination("172.168.3.0/24")
//                        .addAllNexthop(Arrays.asList("10.5.0.5"))
//                        .build()
//        ));
//        OSRouteTable osRouteTable = new OSWindowsRouteTable(tunChannel, virtualRouter);
//        osRouteTable.start();
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        countDownLatch.await();
//    }
//}
