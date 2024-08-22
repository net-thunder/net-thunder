package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.*;
import io.jaspercloud.sdwan.tun.windows.Ics;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
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
            log.info("tunTransport started address={}", config.getIp());
            if (PlatformDependent.isWindows() && config.getIcsEnable()) {
                Ics.enable(config.getLocalAddress(), tunAddress.getIp(), true);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        Ics.enable(config.getLocalAddress(), tunAddress.getIp(), false);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }));
                {
                    String cmd = String.format("netsh interface ipv4 set address name=\"%s\" static %s/%s", tunAddress.getTunName(), tunAddress.getIp(), tunAddress.getMaskBits());
                    int code = ProcessUtil.exec(cmd);
                    CheckInvoke.check(code, 0);
                }
                {
                    String cmd = String.format("netsh interface ipv4 add address name=\"%s\" 192.168.137.1/24", tunAddress.getTunName());
                    int code = ProcessUtil.exec(cmd);
                    CheckInvoke.check(code, 0);
                }
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
