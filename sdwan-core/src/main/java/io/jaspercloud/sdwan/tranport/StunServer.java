package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.stun.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import sun.net.util.IPAddressUtil;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class StunServer implements InitializingBean, DisposableBean {

    private StunServerConfig config;
    private Supplier<ChannelHandler> handler;

    private Channel localChannel;

    public StunServer(StunServerConfig config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    private void processBindRequest(ChannelHandlerContext ctx, StunPacket request) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = request.sender();
        ProtoFamily protoFamily;
        if (IPAddressUtil.isIPv4LiteralAddress(sender.getHostString())) {
            protoFamily = ProtoFamily.IPv4;
        } else if (IPAddressUtil.isIPv6LiteralAddress(sender.getHostString())) {
            protoFamily = ProtoFamily.IPv6;
        } else {
            throw new UnsupportedOperationException();
        }
        StunMessage stunMessage = new StunMessage(MessageType.BindResponse, request.content().getTranId());
        stunMessage.setAttr(AttrType.MappedAddress, new AddressAttr(protoFamily, sender.getHostString(), sender.getPort()));
        stunMessage.setAttr(AttrType.ChangedAddress, new AddressAttr(protoFamily, config.getBindHost(), config.getBindPort()));
        StunPacket response = new StunPacket(stunMessage, request.sender());
        channel.writeAndFlush(response);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        NioEventLoopGroup bossGroup = NioEventLoopFactory.createBossGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(bossGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("stunServer:encoder", new StunEncoder());
                        pipeline.addLast("stunServer:decoder", new StunDecoder());
                        pipeline.addLast("stunServer:process", new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                StunMessage request = packet.content();
                                InetSocketAddress sender = packet.sender();
                                if (MessageType.BindRequest.equals(request.getMessageType())) {
                                    processBindRequest(ctx, packet);
                                } else {
                                    ctx.fireChannelRead(packet);
                                }
                            }
                        });
                        pipeline.addLast("stunServer:handler", handler.get());
                    }
                });
        InetSocketAddress localAddress = new InetSocketAddress(config.getBindPort());
        try {
            localChannel = bootstrap.bind(localAddress).syncUninterruptibly().channel();
            log.info("stun server started: port={}", config.getBindPort());
            localChannel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    bossGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            throw e;
        }
    }

    @Override
    public void destroy() throws Exception {
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }
}
