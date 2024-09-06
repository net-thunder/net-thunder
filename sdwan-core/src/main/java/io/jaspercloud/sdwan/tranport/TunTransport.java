package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.TunChannelConfig;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/12
 */
@Slf4j
public class TunTransport implements TransportLifecycle {

    private TunTransportConfig config;
    private Supplier<ChannelHandler> handler;

    private TunChannel localChannel;

    public TunTransportConfig getConfig() {
        return config;
    }

    public TunChannel getChannel() {
        return localChannel;
    }

    public TunTransport(TunTransportConfig config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    public void writeIpLayerPacket(IpLayerPacket packet) {
        localChannel.writeAndFlush(packet.rebuild());
    }

    public void writeIpPacket(SDWanProtos.IpPacket ipPacket) {
        byte[] bytes = ipPacket.getPayload().toByteArray();
        localChannel.writeAndFlush(ByteBufUtil.toByteBuf(bytes));
    }

    @Override
    public void start() throws Exception {
        EventLoopGroup eventLoopGroup = NioEventLoopFactory.createEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(TunChannel.class)
                .option(TunChannelConfig.MTU, config.getMtu())
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(handler.get());
                    }
                });
        try {
            TunAddress tunAddress = new TunAddress(config.getTunName(), config.getIp(), config.getMaskBits());
            localChannel = (TunChannel) bootstrap.bind(tunAddress).syncUninterruptibly().channel();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (!localChannel.isActive()) {
                        return;
                    }
                    TunTransport.this.stop();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }));
            TunChannel.waitAddress(config.getIp(), 30 * 1000);
            log.info("tunTransport started address={}", config.getIp());
            if (config.getShareNetwork()) {
                NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findIp(config.getLocalAddress());
                localChannel.enableShareNetwork(interfaceInfo.getEthName());
            }
            localChannel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    eventLoopGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            eventLoopGroup.shutdownGracefully();
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        if (null == localChannel) {
            return;
        }
        if (config.getShareNetwork()) {
            NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findIp(config.getLocalAddress());
            localChannel.disableShareNetwork(interfaceInfo.getEthName());
        }
        localChannel.close();
    }

    @Override
    public boolean isRunning() {
        if (null == localChannel) {
            return false;
        }
        return localChannel.isActive();
    }
}
