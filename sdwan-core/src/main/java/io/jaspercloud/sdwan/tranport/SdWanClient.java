package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.util.ShortUUID;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.List;
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

    public int getLocalPort() {
        InetSocketAddress address = (InetSocketAddress) localChannel.localAddress();
        return address.getPort();
    }

    public SdWanClient(SdWanClientConfig config, Supplier<ChannelHandler> handler) {
        this.config = config;
        this.handler = handler;
    }

    private CompletableFuture<SDWanProtos.Message> heart(long timeout) {
        String id = ShortUUID.gen();
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
        String id = ShortUUID.gen();
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
                return regResp;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        });
    }

    public void updateNodeInfo(List<String> list) {
        log.info("updateNodeInfo: {}", StringUtils.join(list));
        SDWanProtos.NodeInfoReq nodeInfoReq = SDWanProtos.NodeInfoReq.newBuilder()
                .addAllAddressUri(list)
                .build();
        String id = ShortUUID.gen();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.UpdateNodeInfoType)
                .setData(nodeInfoReq.toByteString())
                .build();
        localChannel.writeAndFlush(message);
    }

    public CompletableFuture<SDWanProtos.P2pAnswer> offer(SDWanProtos.P2pOffer req, long timeout) {
        String id = ShortUUID.gen();
        log.info("offer: srcVIP={}, dstVIP={}, id={}", req.getSrcVIP(), req.getDstVIP(), id);
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
        log.info("answer: srcVIP={}, dstVIP={}, id={}", req.getSrcVIP(), req.getDstVIP(), id);
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.P2pAnswerType)
                .setData(req.toByteString())
                .build();
        localChannel.writeAndFlush(message);
    }

    public CompletableFuture<SDWanProtos.ServerConfigResp> getConfig(long timeout) {
        String id = ShortUUID.gen();
        SDWanProtos.ServerConfigReq configReq = SDWanProtos.ServerConfigReq.newBuilder()
                .setTenantId(config.getTenantId())
                .build();
        SDWanProtos.Message message = SDWanProtos.Message.newBuilder()
                .setReqId(id)
                .setMode(SDWanProtos.MessageMode.ReqResp)
                .setType(SDWanProtos.MessageTypeCode.ConfigReqType)
                .setData(configReq.toByteString())
                .build();
        CompletableFuture<SDWanProtos.Message> task = AsyncTask.waitTask(id, timeout);
        localChannel.writeAndFlush(message);
        return task.thenApply(result -> {
            try {
                SDWanProtos.ServerConfigResp resp = SDWanProtos.ServerConfigResp.parseFrom(result.getData());
                return resp;
            } catch (Exception e) {
                throw new ProcessException(e.getMessage(), e);
            }
        });
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
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
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
            log.info("SdWanClient started");
            bossGroup.scheduleAtFixedRate(this, 0, config.getHeartTime(), TimeUnit.MILLISECONDS);
            localChannel.closeFuture().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    bossGroup.shutdownGracefully();
                }
            });
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            if (e instanceof ConnectException) {
                throw new ProcessException("ConnectException: " + socketAddress.toString());
            }
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
