package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class SDClientTest {

    @Test
    public void regist() throws Exception {
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .vipCidr("10.5.0.0/24")
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.afterPropertiesSet();
        SdWanClient sdWanClient = new SdWanClient(SdWanClientConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .connectTimeout(3000)
                .heartTime(15 * 1000)
                .build(),
                () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                    }
                });
        sdWanClient.start();
        SDWanProtos.RegistReq req = SDWanProtos.RegistReq.newBuilder()
                .setNodeType(SDWanProtos.NodeTypeCode.SimpleType)
                .setMacAddress("x:x:x:x:x:x")
                .addAddressUri("x.x.x.x")
                .addAddressUri("x.x.x.x")
                .addAddressUri("x.x.x.x")
                .build();
        SDWanProtos.RegistResp regResp = sdWanClient.regist(req, 3000).get();
        System.out.println();
    }

    @Test
    public void bindFixedVip() throws Exception {
        Map<String, String> fixedVipMap = new HashMap<String, String>() {
            {
                put("d1:d2:d3:d4:d5:d6", "10.5.0.35");
            }
        };
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .vipCidr("10.5.0.0/24")
                .fixedVipMap(fixedVipMap)
                .build(), () -> new ChannelInboundHandlerAdapter());
        sdWanServer.afterPropertiesSet();
        SdWanClient sdWanClient = new SdWanClient(SdWanClientConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .connectTimeout(3000)
                .heartTime(15 * 1000)
                .build(),
                () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                    }
                });
        sdWanClient.start();
        SDWanProtos.RegistReq req = SDWanProtos.RegistReq.newBuilder()
                .setNodeType(SDWanProtos.NodeTypeCode.SimpleType)
                .setMacAddress("d1:d2:d3:d4:d5:d6")
                .addAddressUri("x.x.x.x")
                .addAddressUri("x.x.x.x")
                .addAddressUri("x.x.x.x")
                .build();
        SDWanProtos.RegistResp regResp = sdWanClient.regist(req, 3000).get();
        System.out.println();
    }

    @Test
    public void push() throws Exception {
        SdWanServer sdWanServer = new SdWanServer(SdWanServerConfig.builder()
                .port(1800)
                .heartTimeout(30 * 1000)
                .vipCidr("10.5.0.0/24")
                .build(), () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                {
                    SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                            .setVip("x.x.x.x")
                            .build();
                    SDWanProtos.NodeInfoList nodeInfoList = SDWanProtos.NodeInfoList.newBuilder()
                            .addNodeInfo(nodeInfo)
                            .build();
                    SdWanServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.NodeInfoListType, nodeInfoList);
                }
                {
                    SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                            .setVip("x.x.x.x")
                            .build();
                    SdWanServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.NodeOnlineType, nodeInfo);
                }
                {
                    SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                            .setVip("x.x.x.x")
                            .build();
                    SdWanServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.NodeOfflineType, nodeInfo);
                }
                {
                    SDWanProtos.RouteList routeList = SDWanProtos.RouteList.newBuilder()
                            .addRoute(SDWanProtos.Route.newBuilder()
                                    .setDestination("192.168.1.0/24")
                                    .addAllNexthop(Arrays.asList("10.5.0.254"))
                                    .build())
                            .build();
                    SdWanServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.RouteListType, routeList);
                }
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
            }
        });
        sdWanServer.afterPropertiesSet();
        CompletableFuture<SDWanProtos.NodeInfoList> nodeInfoListFuture = new CompletableFuture<>();
        CompletableFuture<SDWanProtos.RouteList> routeListFuture = new CompletableFuture<>();
        CompletableFuture<SDWanProtos.NodeInfo> onlineFuture = new CompletableFuture<>();
        CompletableFuture<SDWanProtos.NodeInfo> offlineFuture = new CompletableFuture<>();
        SdWanClient sdWanClient = new SdWanClient(SdWanClientConfig.builder()
                .controllerServer("127.0.0.1:1800")
                .connectTimeout(3000)
                .heartTime(15 * 1000)
                .build(),
                () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
                        switch (msg.getType().getNumber()) {
                            case SDWanProtos.MessageTypeCode.NodeInfoListType_VALUE: {
                                SDWanProtos.NodeInfoList nodeInfoList = SDWanProtos.NodeInfoList.parseFrom(msg.getData());
                                nodeInfoListFuture.complete(nodeInfoList);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.RouteListType_VALUE: {
                                SDWanProtos.RouteList routeList = SDWanProtos.RouteList.parseFrom(msg.getData());
                                routeListFuture.complete(routeList);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeOnlineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                onlineFuture.complete(nodeInfo);
                                break;
                            }
                            case SDWanProtos.MessageTypeCode.NodeOfflineType_VALUE: {
                                SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.parseFrom(msg.getData());
                                offlineFuture.complete(nodeInfo);
                                break;
                            }
                        }
                    }
                });
        sdWanClient.start();
        SDWanProtos.NodeInfoList nodeInfoList = nodeInfoListFuture.get();
        SDWanProtos.RouteList routeList = routeListFuture.get();
        SDWanProtos.NodeInfo online = onlineFuture.get();
        SDWanProtos.NodeInfo offline = offlineFuture.get();
        System.out.println();
    }
}
