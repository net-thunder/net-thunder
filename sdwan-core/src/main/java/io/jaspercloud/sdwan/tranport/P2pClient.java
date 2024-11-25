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
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
public class P2pClient implements TransportLifecycle {

    private int localPort;
    private Supplier<ChannelHandler> handler;
    private Channel localChannel;

    public int getLocalPort() {
        InetSocketAddress address = (InetSocketAddress) localChannel.localAddress();
        return address.getPort();
    }

    public P2pClient(Supplier<ChannelHandler> handler) {
        this(0, handler);
    }

    public P2pClient(int localPort, Supplier<ChannelHandler> handler) {
        this.localPort = localPort;
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

    public void sendBindOneWay(InetSocketAddress address, String tranId) {
        StunMessage message = new StunMessage(MessageType.BindRequest, tranId);
        StunPacket request = new StunPacket(message, address);
        localChannel.writeAndFlush(request);
    }

    public void sendPingOneWay(InetSocketAddress address, String tranId) {
        StunMessage message = new StunMessage(MessageType.PingRequest, tranId);
        message.setAttr(AttrType.Time, new LongAttr(System.currentTimeMillis()));
        StunPacket request = new StunPacket(message, address);
        localChannel.writeAndFlush(request);
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

    public NatAddress parseNatAddress(InetSocketAddress stunAddress, long timeout) throws Exception {
        StunPacket response = sendBind(stunAddress, timeout).get();
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
            if (null != (response = testChangeBind(stunAddress, true, true, timeout))) {
                return new NatAddress(SDWanProtos.MappingTypeCode.FullCone, mappedAddress1);
            } else if (null != (response = testChangeBind(stunAddress, false, true, timeout))) {
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
            localChannel = bootstrap.bind(new InetSocketAddress("0.0.0.0", localPort)).sync().channel();
            InetSocketAddress localAddress = (InetSocketAddress) localChannel.localAddress();
            log.info("P2pClient started: port={}", localAddress.getPort());
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
}
