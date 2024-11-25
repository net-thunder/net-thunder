package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.stun.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class P2pClientTest {

    @Test
    public void stun() throws Exception {
        StunServerConfig config = StunServerConfig.builder()
                .bindHost("127.0.0.1")
                .bindPort(1000)
                .build();
        StunServer stunServer = new StunServer(config, () -> new ChannelInboundHandlerAdapter());
        stunServer.start();
        P2pClient p2pClient = new P2pClient(3000, () -> new ChannelInboundHandlerAdapter());
        p2pClient.start();
        NatAddress natAddress = p2pClient.parseNatAddress(new InetSocketAddress("127.0.0.1", 3478), 3000);
        System.out.println();
    }

    @Test
    public void transfer() throws Exception {
        CompletableFuture<StunPacket> future = new CompletableFuture<>();
        P2pClient p2pClient1 = new P2pClient(1001, () -> new SimpleChannelInboundHandler<StunPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket msg) throws Exception {
                future.complete(msg);
            }
        });
        p2pClient1.start();
        P2pClient p2pClient2 = new P2pClient(1002, () -> new ChannelInboundHandlerAdapter());
        p2pClient2.start();
        p2pClient2.transfer("127.0.0.1", "p2p", new InetSocketAddress("127.0.0.1", 1001), "test".getBytes());
        StunPacket stunPacket = future.get();
        BytesAttr attr = stunPacket.content().getAttr(AttrType.Data);
        byte[] data = attr.getData();
        String text = new String(data);
        System.out.println("text: " + text);
    }

    @Test
    public void ping() throws Exception {
        P2pClient p2pClient1 = new P2pClient(1001, () -> new SimpleChannelInboundHandler<StunPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket msg) throws Exception {
                System.out.println();
            }
        });
        p2pClient1.start();
        P2pClient p2pClient2 = new P2pClient(1002, () -> new SimpleChannelInboundHandler<StunPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, StunPacket packet) throws Exception {
                StunMessage message = packet.content();
                message.setMessageType(MessageType.PingResponse);
                StunPacket response = new StunPacket(message, packet.sender());
                ctx.writeAndFlush(response);
            }
        });
        p2pClient2.start();
        StunPacket stunPacket = p2pClient1.ping(new InetSocketAddress("127.0.0.1", 1002), 3000).get();
        System.out.println();
    }
}
