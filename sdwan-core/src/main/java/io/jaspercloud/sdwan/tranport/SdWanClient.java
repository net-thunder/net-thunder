package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class SdWanClient implements TransportLifecycle, Runnable {

    private SdWanClientConfig config;
    private Supplier<ChannelHandler> handler;

    private Channel localChannel;

    public Channel getChannel() {
        return localChannel;
    }

    public SdWanClient(SdWanClientConfig config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    private CompletableFuture<SDWanProtos.Message> heart(long timeout) {
        String id = UUID.randomUUID().toString();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.HeartType)
                .build();
        CompletableFuture<SDWanProtos.Message> task = AsyncTask.waitTask(id, timeout);
        localChannel.writeAndFlush(message);
        return task;
    }

    public CompletableFuture<SDWanProtos.RegistResp> regist(SDWanProtos.RegistReq req, long timeout) {
        String id = UUID.randomUUID().toString();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.RegistReqType)
                .setData(req.toByteString())
                .build();
        CompletableFuture<SDWanProtos.Message> task = AsyncTask.waitTask(id, timeout);
        localChannel.writeAndFlush(message);
        return task.thenApply(result -> {
            try {
                SDWanProtos.RegistResp regResp = SDWanProtos.RegistResp.parseFrom(result.getData());
                log.info("regist vip={}", regResp.getVip());
                return regResp;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<SDWanProtos.P2pAnswer> offer(SDWanProtos.P2pOffer req, long timeout) {
        String id = UUID.randomUUID().toString();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.P2pOfferType)
                .setData(req.toByteString())
                .build();
        CompletableFuture<SDWanProtos.Message> task = AsyncTask.waitTask(id, timeout);
        localChannel.writeAndFlush(message);
        return task.thenApply(result -> {
            try {
                SDWanProtos.P2pAnswer p2pAnswer = SDWanProtos.P2pAnswer.parseFrom(result.getData());
                return p2pAnswer;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    public void answer(String id, SDWanProtos.P2pAnswer req) {
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.P2pAnswerType)
                .setData(req.toByteString())
                .build();
        localChannel.writeAndFlush(message);
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
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(bossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new TcpLogHandler("sdwan"));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder());
                        pipeline.addLast(new ProtobufDecoder(SDWanProtos.Message.getDefaultInstance()));
                        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                        pipeline.addLast(new ProtobufEncoder());
                        pipeline.addLast(new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                                if (SDWanProtos.MessageMode.ReqResp.equals(msg.getMode())) {
                                    AsyncTask.completeTask(msg.getReqId(), msg);
                                } else {
                                    ctx.fireChannelRead(msg);
                                }
                            }
                        });
                        pipeline.addLast("sdwanClient:process", handler.get());
                    }
                });
        InetSocketAddress socketAddress = SocketAddressUtil.parse(config.getControllerServer());
        try {
            localChannel = bootstrap.connect(socketAddress).syncUninterruptibly().channel();
            log.info("sdwan client started");
            bossGroup.scheduleAtFixedRate(this, 0, config.getHeartTime(), TimeUnit.MILLISECONDS);
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
        if (null == localChannel) {
            return;
        }
        localChannel.close();
    }

    @Override
    public void run() {
        try {
            heart(3000).get();
        } catch (ExecutionException e) {
            localChannel.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
