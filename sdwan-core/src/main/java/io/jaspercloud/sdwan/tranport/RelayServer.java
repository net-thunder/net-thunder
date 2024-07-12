package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class RelayServer implements InitializingBean, DisposableBean, Runnable {

    private RelayServerConfig config;
    private Supplier<ChannelHandler> handler;

    private Channel localChannel;
    private Map<String, InetSocketAddress> transferMap = new ConcurrentHashMap<>();
    private Map<String, Heart> clientTokenMap = new ConcurrentHashMap<>();

    public RelayServer(RelayServerConfig config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    private void processTransfer(ChannelHandlerContext ctx, StunPacket request) {
        Channel channel = ctx.channel();
        StunMessage stunMessage = request.content();
        StringAttr tokenAttr = stunMessage.getAttr(AttrType.RelayToken);
        InetSocketAddress address = transferMap.get(tokenAttr.getData());
        if (null == address) {
            return;
        }
        StunPacket response = new StunPacket(stunMessage, address);
        channel.writeAndFlush(response);
    }

    private void processBindRelayRequest(ChannelHandlerContext ctx, StunPacket request) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = request.sender();
        String addr = SocketAddressUtil.toAddress(sender);
        Heart heart = clientTokenMap.computeIfAbsent(addr, key -> {
            String token = UUID.randomUUID().toString();
            transferMap.put(token, sender);
            return Heart.builder()
                    .lastHeartTime(System.currentTimeMillis())
                    .token(token)
                    .build();
        });
        heart.setLastHeartTime(System.currentTimeMillis());
        StunMessage stunMessage = new StunMessage(MessageType.BindRelayResponse, request.content().getTranId());
        stunMessage.setAttr(AttrType.RelayToken, new StringAttr(heart.getToken()));
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
                        pipeline.addLast("relayServer:encoder", new StunEncoder());
                        pipeline.addLast("relayServer:decoder", new StunDecoder());
                        pipeline.addLast("relayServer:process", new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                StunMessage request = packet.content();
                                InetSocketAddress sender = packet.sender();
                                if (MessageType.BindRelayRequest.equals(request.getMessageType())) {
                                    processBindRelayRequest(ctx, packet);
                                } else if (MessageType.Transfer.equals(request.getMessageType())) {
                                    processTransfer(ctx, packet);
                                } else {
                                    ctx.fireChannelRead(packet);
                                }
                            }
                        });
                        pipeline.addLast("relayServer:handler", handler.get());
                    }
                });
        InetSocketAddress localAddress = new InetSocketAddress("0.0.0.0", config.getBindPort());
        try {
            localChannel = bootstrap.bind(localAddress).syncUninterruptibly().channel();
            log.info("relay server started: port={}", config.getBindPort());
            bossGroup.scheduleAtFixedRate(this, 0, 2 * config.getHeartTimeout(), TimeUnit.MILLISECONDS);
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
        localChannel.close();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Heart>> iterator = clientTokenMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Heart> next = iterator.next();
            String addr = next.getKey();
            Heart heart = next.getValue();
            long diff = now - heart.getLastHeartTime();
            if (diff > config.getHeartTimeout()) {
                log.info("timeout: addr={}", addr);
                clientTokenMap.remove(addr);
                transferMap.remove(next.getValue().getToken());
            }
        }
    }

    @Builder
    @Getter
    @Setter
    public static class Heart {


        private Long lastHeartTime;
        private String token;
    }
}
