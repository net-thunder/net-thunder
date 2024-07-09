package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.TunChannelConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.util.concurrent.CountDownLatch;

public class TunChannelTest {

    public static void main(String[] args) throws Exception {
        Bootstrap bootstrap = new Bootstrap()
                .group(new DefaultEventLoopGroup())
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, 1500)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(final Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                Ipv4Packet ipv4Packet = Ipv4Packet.decode(byteBuf);
                                System.out.println();
                            }
                        });
                    }
                });
        ChannelFuture future = bootstrap.bind(new TunAddress("tun", "192.168.1.1", 24));
        TunChannel channel = (TunChannel) future.syncUninterruptibly().channel();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }

}
