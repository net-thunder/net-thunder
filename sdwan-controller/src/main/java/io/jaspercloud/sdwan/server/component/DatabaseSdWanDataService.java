package io.jaspercloud.sdwan.server.component;

import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.config.*;
import io.jaspercloud.sdwan.tranport.service.SdWanDataService;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseSdWanDataService implements SdWanDataService {

    @Resource
    private TenantService tenantService;

    @Resource
    private NodeService nodeService;

    @Override
    public boolean hasTenant(String tenantCode) {
        TenantResponse tenantResponse = tenantService.queryByTenantCode(tenantCode);
        return null != tenantResponse;
    }

    @Override
    public TenantConfig getTenantConfig(String tenantCode) {
        TenantResponse tenantResponse = tenantService.queryByTenantCode(tenantCode);
        if (null == tenantResponse) {
            return null;
        }
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setIpPool(Cidr.parseCidr(tenantResponse.getCidr()));
        tenantConfig.setStunServerList(tenantResponse.getStunServerList());
        tenantConfig.setRelayServerList(tenantResponse.getRelayServerList());
        return tenantConfig;
    }

    @Override
    public NodeConfig assignNodeInfo(Channel channel, SDWanProtos.RegistReq registReq) {
        TenantResponse tenantResponse = tenantService.queryByTenantCode(registReq.getTenantId());
        if (null == tenantResponse) {
            throw new ProcessException("not found tenant");
        }
        TenantContextHandler.setTenantId(tenantResponse.getId());
        try {
            NodeDetailResponse detailResponse = nodeService.assignNodeInfo(tenantResponse.getId(), registReq);
            NodeConfig nodeConfig = new NodeConfig();
            nodeConfig.setMac(detailResponse.getMac());
            nodeConfig.setVip(detailResponse.getVip());
            if (CollectionUtil.isNotEmpty(detailResponse.getRouteList())) {
                List<RouteConfig> collect = detailResponse.getRouteList().stream()
                        .map(route -> {
                            List<String> vipList = nodeService.queryByIdList(route.getNodeIdList())
                                    .stream()
                                    .map(e -> e.getVip())
                                    .filter(e -> null != e)
                                    .collect(Collectors.toList());
                            RouteConfig config = new RouteConfig();
                            config.setDestination(route.getDestination());
                            config.setNexthop(vipList);
                            return config;
                        }).collect(Collectors.toList());
                nodeConfig.setRouteConfigList(collect);
            } else {
                nodeConfig.setRouteConfigList(Collections.emptyList());
            }
            if (CollectionUtil.isNotEmpty(detailResponse.getRouteRuleList())) {
                List<RouteRuleConfig> collect = detailResponse.getRouteRuleList().stream()
                        .map(rule -> {
                            RouteRuleConfig routeRuleConfig = new RouteRuleConfig();
                            routeRuleConfig.setStrategy(rule.getStrategy());
                            routeRuleConfig.setDirection(rule.getDirection());
                            routeRuleConfig.setLevel(rule.getLevel());
                            routeRuleConfig.setRuleList(rule.getRuleList());
                            return routeRuleConfig;
                        }).collect(Collectors.toList());
                nodeConfig.setRouteRuleConfigList(collect);
            } else {
                nodeConfig.setRouteRuleConfigList(Collections.emptyList());
            }
            if (CollectionUtil.isNotEmpty(detailResponse.getVnatList())) {
                List<VNATConfig> collect = detailResponse.getVnatList().stream()
                        .map(vnat -> {
                            VNATConfig config = new VNATConfig();
                            config.setSrcCidr(vnat.getSrcCidr());
                            config.setDstCidr(vnat.getDstCidr());
                            List<String> vipList = nodeService.queryByIdList(vnat.getNodeIdList())
                                    .stream()
                                    .map(e -> e.getVip())
                                    .filter(e -> null != e)
                                    .collect(Collectors.toList());
                            config.setVipList(vipList);
                            return config;
                        }).collect(Collectors.toList());
                nodeConfig.setVnatConfigList(collect);
            } else {
                nodeConfig.setVnatConfigList(Collections.emptyList());
            }
            return nodeConfig;
        } finally {
            TenantContextHandler.remove();
        }
    }

    @Override
    public List<String> getStunServerList(String tenantCode) {
        TenantResponse tenantResponse = tenantService.queryByTenantCode(tenantCode);
        if (null == tenantResponse) {
            throw new ProcessException("not found tenant");
        }
        List<String> list = tenantResponse.getStunServerList();
        return list;
    }

    @Override
    public List<String> getRelayServerList(String tenantCode) {
        TenantResponse tenantResponse = tenantService.queryByTenantCode(tenantCode);
        if (null == tenantResponse) {
            throw new ProcessException("not found tenant");
        }
        List<String> list = tenantResponse.getRelayServerList();
        return list;
    }
}
