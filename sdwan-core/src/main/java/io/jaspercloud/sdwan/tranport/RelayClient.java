package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class RelayClient implements TransportLifecycle {

    private Config config;
    private Supplier<ChannelHandler> handler;
    private Channel localChannel;

    public RelayClient() {
        this(() -> {
            return new ChannelInboundHandlerAdapter();
        });
    }

    public RelayClient(Supplier<ChannelHandler> handler) {
        this(new Config(), handler);
    }

    public RelayClient(Config config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    private CompletableFuture<StunPacket> invokeAsync(StunPacket request, long timeout) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        localChannel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> ping(InetSocketAddress socketAddress, String token, long timeout) {
        StunMessage message = new StunMessage(MessageType.PingRequest);
        message.setAttr(AttrType.RelayToken, new StringAttr(token));
        message.setAttr(AttrType.Time, new LongAttr(System.currentTimeMillis()));
        StunPacket request = new StunPacket(message, socketAddress);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public CompletableFuture<String> regist(InetSocketAddress socketAddress, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRelayRequest);
        StunPacket request = new StunPacket(message, socketAddress);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future.thenApply(result -> {
            StunMessage stunMessage = result.content();
            StringAttr attr = stunMessage.getAttr(AttrType.RelayToken);
            String token = attr.getData();
            return token;
        });
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRelayRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public void sendBindOneWay(InetSocketAddress socketAddress, String tranId) {
        StunMessage message = new StunMessage(MessageType.BindRelayRequest, tranId);
        StunPacket request = new StunPacket(message, socketAddress);
        localChannel.writeAndFlush(request);
    }

    public void sendPingOneWay(InetSocketAddress socketAddress, String token, String tranId) {
        StunMessage message = new StunMessage(MessageType.PingRequest, tranId);
        message.setAttr(AttrType.RelayToken, new StringAttr(token));
        message.setAttr(AttrType.Time, new LongAttr(System.currentTimeMillis()));
        StunPacket request = new StunPacket(message, socketAddress);
        localChannel.writeAndFlush(request);
    }

    public void transfer(String vip, String type, InetSocketAddress address, String token, byte[] bytes) {
        if (config.getShowICELog()) {
            log.info("relay send transfer: type={}, address={}", type, SocketAddressUtil.toAddress(address));
        }
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.setAttr(AttrType.TransferType, new StringAttr(type));
        message.setAttr(AttrType.RelayToken, new StringAttr(token));
        message.setAttr(AttrType.SrcVip, new StringAttr(vip));
        message.setAttr(AttrType.Data, new BytesAttr(bytes));
        StunPacket request = new StunPacket(message, address);
        localChannel.writeAndFlush(request);
    }

    @Override
    public boolean isRunning() {
        if (null == localChannel) {
            return false;
        }
        return localChannel.isActive();
    }

    @Override
    public void start() {
        NioEventLoopGroup bossGroup = NioEventLoopFactory.createBossGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(bossGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("relayClient:encoder", new StunEncoder());
                        pipeline.addLast("relayClient:decoder", new StunDecoder());
                        pipeline.addLast("relayClient:task", new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                StunMessage request = packet.content();
                                InetSocketAddress sender = packet.sender();
                                if (MessageType.BindRelayResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.RefreshRelayResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.PingResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else {
                                    ctx.fireChannelRead(packet);
                                }
                            }
                        });
                        pipeline.addLast("relayClient:handler", handler.get());
                    }
                });
        try {
            localChannel = bootstrap.bind(new InetSocketAddress("0.0.0.0", config.getLocalPort())).syncUninterruptibly().channel();
            InetSocketAddress localAddress = (InetSocketAddress) localChannel.localAddress();
            log.info("RelayClient started: port={}", localAddress.getPort());
            localChannel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    bossGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            throw new ProcessException(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        log.info("RelayClient stopping");
        if (null == localChannel) {
            return;
        }
        localChannel.close();
        log.info("RelayClient stopped");
    }

    @Getter
    @Setter
    public static class Config {

        private int localPort = 0;
        private Boolean showICELog = false;
    }
}
