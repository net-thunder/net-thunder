package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.service.LocalConfigSdWanDataService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/2
 */
public class SDClientTest {

    @Test
    public void regist() throws Exception {
        Map<String, ControllerServerConfig.TenantConfig> tenantConfigMap = Collections.singletonMap("tenant1", ControllerServerConfig.TenantConfig.builder()
                .vipCidr("10.5.0.0/24")
                .build());
        ControllerServerConfig config = new ControllerServerConfig();
        config.setTenantConfig(tenantConfigMap);
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        controllerServer.start();
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
        List<ControllerServerConfig.FixVip> fixVipList = fixedVipMap.entrySet().stream().map(e -> {
            return ControllerServerConfig.FixVip.builder().mac(e.getKey()).vip(e.getValue()).build();
        }).collect(Collectors.toList());
        Map<String, ControllerServerConfig.TenantConfig> tenantConfigMap = Collections.singletonMap("tenant1", ControllerServerConfig.TenantConfig.builder()
                .vipCidr("10.5.0.0/24")
                .fixedVipList(Collections.emptyList())
                .build());
        ControllerServerConfig config = new ControllerServerConfig();
        config.setTenantConfig(tenantConfigMap);
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new ChannelInboundHandlerAdapter());
        controllerServer.start();
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
        Map<String, ControllerServerConfig.TenantConfig> tenantConfigMap = Collections.singletonMap("tenant1", ControllerServerConfig.TenantConfig.builder()
                .vipCidr("10.5.0.0/24")
                .build());
        ControllerServerConfig config = new ControllerServerConfig();
        config.setTenantConfig(tenantConfigMap);
        LocalConfigSdWanDataService dataService = new LocalConfigSdWanDataService(config);
        ControllerServer controllerServer = new ControllerServer(config, dataService, () -> new SimpleChannelInboundHandler<SDWanProtos.Message>() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                {
                    SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                            .setVip("x.x.x.x")
                            .build();
                    SDWanProtos.NodeInfoList nodeInfoList = SDWanProtos.NodeInfoList.newBuilder()
                            .addNodeInfo(nodeInfo)
                            .build();
                    ControllerServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.NodeInfoListType, nodeInfoList);
                }
                {
                    SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                            .setVip("x.x.x.x")
                            .build();
                    ControllerServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.NodeOnlineType, nodeInfo);
                }
                {
                    SDWanProtos.NodeInfo nodeInfo = SDWanProtos.NodeInfo.newBuilder()
                            .setVip("x.x.x.x")
                            .build();
                    ControllerServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.NodeOfflineType, nodeInfo);
                }
                {
                    SDWanProtos.RouteList routeList = SDWanProtos.RouteList.newBuilder()
                            .addRoute(SDWanProtos.Route.newBuilder()
                                    .setDestination("192.168.1.0/24")
                                    .addAllNexthop(Arrays.asList("10.5.0.254"))
                                    .build())
                            .build();
                    ControllerServer.push(ctx.channel(), SDWanProtos.MessageTypeCode.RouteListType, routeList);
                }
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SDWanProtos.Message msg) throws Exception {
            }
        });
        controllerServer.start();
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
