package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class RelayClient implements TransportLifecycle, Runnable {

    private InetSocketAddress relayAddress;
    private int port;
    private long heartTime;
    private Supplier<ChannelHandler> handler;

    private Channel localChannel;
    private String curToken;

    public String getCurToken() {
        return curToken;
    }

    public int getLocalPort() {
        InetSocketAddress address = (InetSocketAddress) localChannel.localAddress();
        return address.getPort();
    }

    public RelayClient(String relayServer, long heartTime, Supplier<ChannelHandler> handler) {
        this(relayServer, 0, heartTime, handler);
    }

    public RelayClient(String relayServer, int port, long heartTime, Supplier<ChannelHandler> handler) {
        this.relayAddress = SocketAddressUtil.parse(relayServer);
        this.port = port;
        this.heartTime = heartTime;
        this.handler = handler;
    }

    private CompletableFuture<StunPacket> invokeAsync(StunPacket request, long timeout) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        localChannel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> ping(String token, long timeout) {
        StunMessage message = new StunMessage(MessageType.PingRequest);
        message.setAttr(AttrType.RelayToken, new StringAttr(token));
        message.setAttr(AttrType.Time, new LongAttr(System.currentTimeMillis()));
        StunPacket request = new StunPacket(message, relayAddress);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public CompletableFuture<String> regist(long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRelayRequest);
        StunPacket request = new StunPacket(message, relayAddress);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future.thenApply(result -> {
            StunMessage stunMessage = result.content();
            StringAttr attr = stunMessage.getAttr(AttrType.RelayToken);
            String token = attr.getData();
            return token;
        });
    }

    public void transfer(String vip, InetSocketAddress address, String token, byte[] bytes) {
        if (log.isTraceEnabled()) {
            log.trace("relay send transfer: {}", SocketAddressUtil.toAddress(address));
        }
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.setAttr(AttrType.TransferType, new StringAttr("relay"));
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
    public void start() throws Exception {
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
            localChannel = bootstrap.bind(new InetSocketAddress("0.0.0.0", port)).syncUninterruptibly().channel();
            InetSocketAddress localAddress = (InetSocketAddress) localChannel.localAddress();
            curToken = regist(3000).get();
            log.info("RelayClient started: port={}, token={}", localAddress.getPort(), curToken);
            bossGroup.scheduleAtFixedRate(this, 0, heartTime, TimeUnit.MILLISECONDS);
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
    public void stop() throws Exception {
        log.info("RelayClient stopping");
        if (null == localChannel) {
            return;
        }
        localChannel.close();
        log.info("RelayClient stopped");
    }

    @Override
    public void run() {
        try {
            String token = regist(3000).get();
            if (!StringUtils.equals(token, curToken)) {
                localChannel.close();
            }
        } catch (ExecutionException e) {
            localChannel.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
