package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.stun.*;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.util.IPUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
public class P2pClient implements TransportLifecycle, Runnable {

    private int port;
    private long heartTime;
    private long timeout;
    private Supplier<ChannelHandler> handler;

    private List<StunPing> stunServerList = new ArrayList<>();
    private Channel localChannel;

    public int getLocalPort() {
        InetSocketAddress address = (InetSocketAddress) localChannel.localAddress();
        return address.getPort();
    }

    public P2pClient(long heartTime, Supplier<ChannelHandler> handler) {
        this(0, heartTime, 3000, handler);
    }

    public P2pClient(int port, long heartTime, long timeout, Supplier<ChannelHandler> handler) {
        this.port = port;
        this.heartTime = heartTime;
        this.timeout = timeout;
        this.handler = handler;
    }

    private void processBindRequest(ChannelHandlerContext ctx, StunPacket request) {
        Channel channel = ctx.channel();
        InetSocketAddress sender = request.sender();
        if (!IPUtil.isIPv4(sender.getHostString())) {
            throw new UnsupportedOperationException();
        }
        ProtoFamily protoFamily = ProtoFamily.IPv4;
        StunMessage stunMessage = new StunMessage(MessageType.BindResponse, request.content().getTranId());
        stunMessage.setAttr(AttrType.MappedAddress, new AddressAttr(protoFamily, sender.getHostString(), sender.getPort()));
        StunPacket response = new StunPacket(stunMessage, request.sender());
        channel.writeAndFlush(response);
    }

    private CompletableFuture<StunPacket> invokeAsync(StunPacket request, long timeout) {
        CompletableFuture<StunPacket> future = AsyncTask.waitTask(request.content().getTranId(), timeout);
        localChannel.writeAndFlush(request);
        return future;
    }

    public CompletableFuture<StunPacket> sendBind(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public CompletableFuture<StunPacket> ping(InetSocketAddress address, long timeout) {
        StunMessage message = new StunMessage(MessageType.PingRequest);
        message.setAttr(AttrType.Time, new LongAttr(System.currentTimeMillis()));
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    public void transfer(String vip, InetSocketAddress toAddress, byte[] bytes) {
        if (log.isTraceEnabled()) {
            log.trace("p2p send transfer: {}", SocketAddressUtil.toAddress(toAddress));
        }
        StunMessage message = new StunMessage(MessageType.Transfer);
        message.setAttr(AttrType.TransferType, new StringAttr("p2p"));
        message.setAttr(AttrType.SrcVip, new StringAttr(vip));
        message.setAttr(AttrType.Data, new BytesAttr(bytes));
        StunPacket request = new StunPacket(message, toAddress);
        localChannel.writeAndFlush(request);
    }

    public NatAddress addStunServer(String stunServer) throws Exception {
        NatAddress natAddress = parseNatAddress(stunServer, timeout);
        StunPing stunPing = new StunPing(stunServer, natAddress);
        stunServerList.add(stunPing);
        return natAddress;
    }

    public NatAddress parseNatAddress(String stunServer, long timeout) throws Exception {
        InetSocketAddress remote = SocketAddressUtil.parse(stunServer);
        StunPacket response = sendBind(remote, timeout).get();
        Map<AttrType, Attr> attrs = response.content().getAttrs();
        AddressAttr changedAddressAttr = (AddressAttr) attrs.get(AttrType.ChangedAddress);
        if (null == changedAddressAttr) {
            AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
            InetSocketAddress mappedAddress = mappedAddressAttr.getAddress();
            return new NatAddress(SDWanProtos.MappingTypeCode.FullCone, mappedAddress);
        } else {
            InetSocketAddress changedAddress = changedAddressAttr.getAddress();
            AddressAttr mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
            InetSocketAddress mappedAddress1 = mappedAddressAttr.getAddress();
            if (null != (response = testChangeBind(remote, true, true, timeout))) {
                return new NatAddress(SDWanProtos.MappingTypeCode.FullCone, mappedAddress1);
            } else if (null != (response = testChangeBind(remote, false, true, timeout))) {
                return new NatAddress(SDWanProtos.MappingTypeCode.RestrictedCone, mappedAddress1);
            }
            try {
                response = sendBind(changedAddress, timeout).get();
                attrs = response.content().getAttrs();
                mappedAddressAttr = (AddressAttr) attrs.get(AttrType.MappedAddress);
                InetSocketAddress mappedAddress2 = mappedAddressAttr.getAddress();
                if (Objects.equals(mappedAddress1, mappedAddress2)) {
                    return new NatAddress(SDWanProtos.MappingTypeCode.PortRestrictedCone, mappedAddress1);
                } else {
                    return new NatAddress(SDWanProtos.MappingTypeCode.Symmetric, mappedAddress1);
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    return new NatAddress(SDWanProtos.MappingTypeCode.Symmetric, mappedAddress1);
                } else {
                    throw e;
                }
            }
        }
    }

    private CompletableFuture<StunPacket> sendChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort, long timeout) {
        StunMessage message = new StunMessage(MessageType.BindRequest);
        ChangeRequestAttr changeRequestAttr = new ChangeRequestAttr(changeIP, changePort);
        message.getAttrs().put(AttrType.ChangeRequest, changeRequestAttr);
        StunPacket request = new StunPacket(message, address);
        CompletableFuture<StunPacket> future = invokeAsync(request, timeout);
        return future;
    }

    private StunPacket testChangeBind(InetSocketAddress address, boolean changeIP, boolean changePort, long timeout) {
        try {
            StunPacket response = sendChangeBind(address, changeIP, changePort, timeout).get();
            return response;
        } catch (Throwable e) {
            return null;
        }
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
                        pipeline.addLast("p2pClient:encoder", new StunEncoder());
                        pipeline.addLast("p2pClient:decoder", new StunDecoder());
                        pipeline.addLast("p2pClient:task", new SimpleChannelInboundHandler<StunPacket>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                                StunMessage request = packet.content();
                                InetSocketAddress sender = packet.sender();
                                if (MessageType.BindRequest.equals(request.getMessageType())) {
                                    processBindRequest(ctx, packet);
                                } else if (MessageType.BindResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.BindRelayResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else if (MessageType.PingResponse.equals(request.getMessageType())) {
                                    AsyncTask.completeTask(request.getTranId(), packet);
                                } else {
                                    ctx.fireChannelRead(packet);
                                }
                            }
                        });
                        pipeline.addLast("p2pClient:handler", handler.get());
                    }
                });
        try {
            localChannel = bootstrap.bind(new InetSocketAddress("0.0.0.0", port)).sync().channel();
            InetSocketAddress localAddress = (InetSocketAddress) localChannel.localAddress();
            log.info("P2pClient started: port={}", localAddress.getPort());
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
        log.info("P2pClient stopping");
        if (null == localChannel) {
            return;
        }
        localChannel.close();
        log.info("P2pClient stopped");
    }

    @Override
    public void run() {
        for (StunPing ping : stunServerList) {
            try {
                boolean ret = ping.ping(timeout);
                if (!ret) {
                    log.error("stun server error: {}", ping.getStunServer());
                    localChannel.close();
                }
            } catch (ExecutionException e) {
                log.error("stun server error: {}", ping.getStunServer());
                localChannel.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Getter
    @Setter
    private class StunPing {

        private String stunServer;
        private NatAddress curNatAddress;

        public StunPing(String stunServer, NatAddress curNatAddress) {
            this.stunServer = stunServer;
            this.curNatAddress = curNatAddress;
        }

        public boolean ping(long timeout) throws Exception {
            NatAddress natAddress = parseNatAddress(stunServer, timeout);
            boolean eq = natAddress.equals(curNatAddress);
            return eq;
        }
    }
}
