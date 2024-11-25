package io.jaspercloud.sdwan.tranport.service;

import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;
import io.jaspercloud.sdwan.tranport.config.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class LocalConfigSdWanDataService implements SdWanDataService {

    private Map<String, TenantSpace> tenantSpaceMap = new ConcurrentHashMap<>();

    public LocalConfigSdWanDataService(SdWanServerConfig serverConfig) {
        if (CollectionUtil.isNotEmpty(serverConfig.getTenantConfig())) {
            Map<String, SdWanServerConfig.TenantConfig> tenantConfig = serverConfig.getTenantConfig();
            tenantConfig.forEach((key, config) -> {
                TenantSpace tenantSpace = new TenantSpace();
                if (CollectionUtil.isNotEmpty(config.getStunServerList())) {
                    tenantSpace.setStunServerList(config.getStunServerList());
                }
                if (CollectionUtil.isNotEmpty(config.getRelayServerList())) {
                    tenantSpace.setRelayServerList(config.getRelayServerList());
                }
                if (CollectionUtil.isNotEmpty(config.getFixedVipList())) {
                    tenantSpace.setFixedVipMap(config.getFixedVipList()
                            .stream().collect(Collectors.toMap(e -> e.getMac(), e -> e.getVip())));
                }
                if (CollectionUtil.isNotEmpty(config.getRouteList())) {
                    List<RouteConfig> collect = config.getRouteList().stream()
                            .map(e -> {
                                RouteConfig routeConfig = new RouteConfig();
                                routeConfig.setDestination(e.getDestination());
                                routeConfig.setNexthop(e.getNexthop());
                                return routeConfig;
                            }).collect(Collectors.toList());
                    tenantSpace.setRouteList(collect);
                }
                if (CollectionUtil.isNotEmpty(config.getRouteRuleList())) {
                    List<RouteRuleConfig> collect = config.getRouteRuleList().stream()
                            .map(e -> {
                                RouteRuleConfig routeRuleConfig = new RouteRuleConfig();
                                routeRuleConfig.setStrategy(e.getStrategy());
                                routeRuleConfig.setDirection(e.getDirection());
                                routeRuleConfig.setRuleList(e.getRuleList());
                                routeRuleConfig.setLevel(e.getLevel());
                                return routeRuleConfig;
                            }).collect(Collectors.toList());
                    tenantSpace.setRouteRuleList(collect);
                }
                if (CollectionUtil.isNotEmpty(config.getVnatList())) {
                    List<VNATConfig> collect = config.getVnatList().stream()
                            .map(e -> {
                                VNATConfig vnatConfig = new VNATConfig();
                                vnatConfig.setSrcCidr(e.getSrc());
                                vnatConfig.setDstCidr(e.getDst());
                                return vnatConfig;
                            }).collect(Collectors.toList());
                    tenantSpace.setVnatList(collect);
                }
                Cidr ipPool = Cidr.parseCidr(config.getVipCidr());
                tenantSpace.setIpPool(ipPool);
                Map<String, AtomicReference<Channel>> bindIPMap = tenantSpace.getBindIPMap();
                ipPool.availableIpList().forEach(vip -> {
                    bindIPMap.put(vip, new AtomicReference<>());
                });
                tenantSpace.setBindIPMap(bindIPMap);
                tenantSpaceMap.put(key, tenantSpace);
            });
        }
    }

    @Override
    public boolean hasTenant(String tenantCode) {
        boolean containsKey = tenantSpaceMap.containsKey(tenantCode);
        return containsKey;
    }

    @Override
    public TenantConfig getTenantConfig(String tenantCode) {
        TenantSpace tenantSpace = tenantSpaceMap.get(tenantCode);
        if (null == tenantSpace) {
            throw new ProcessException("not found tenant");
        }
        TenantConfig tenantConfig = new TenantConfig();
        tenantConfig.setIpPool(tenantSpace.getIpPool());
        tenantConfig.setStunServerList(tenantSpace.getStunServerList());
        tenantConfig.setRelayServerList(tenantSpace.getRelayServerList());
        return tenantConfig;
    }

    @Override
    public NodeConfig applyNodeInfo(Channel channel, String tenantCode, String macAddress) {
        TenantSpace tenantSpace = tenantSpaceMap.get(tenantCode);
        if (null == tenantSpace) {
            throw new ProcessException("not found tenant");
        }
        String vip = applyVip(channel, tenantSpace, macAddress);
        NodeConfig nodeConfig = new NodeConfig();
        nodeConfig.setMac(macAddress);
        nodeConfig.setVip(vip);
        nodeConfig.setRouteConfigList(tenantSpace.getRouteList());
        nodeConfig.setRouteRuleConfigList(tenantSpace.getRouteRuleList());
        nodeConfig.setVnatConfigList(tenantSpace.getVnatList());
        return nodeConfig;
    }

    @Override
    public List<String> getStunServerList(String tenantCode) {
        TenantSpace tenantSpace = tenantSpaceMap.get(tenantCode);
        if (null == tenantSpace) {
            throw new ProcessException("not found tenant");
        }
        List<String> list = tenantSpace.getStunServerList();
        return list;
    }

    @Override
    public List<String> getRelayServerList(String tenantCode) {
        TenantSpace tenantSpace = tenantSpaceMap.get(tenantCode);
        if (null == tenantSpace) {
            throw new ProcessException("not found tenant");
        }
        List<String> list = tenantSpace.getRelayServerList();
        return list;
    }

    private String applyVip(Channel channel, TenantSpace tenantSpace, String macAddress) {
        Map<String, String> fixedVipMap = tenantSpace.getFixedVipMap();
        Map<String, AtomicReference<Channel>> bindIPMap = tenantSpace.getBindIPMap();
        String fixedIp = fixedVipMap.get(macAddress);
        if (null != fixedIp) {
            if (!bindIPMap.get(fixedIp).compareAndSet(null, channel)) {
                throw new ProcessCodeException(SDWanProtos.MessageCode.VipBound_VALUE);
            }
            bindCloseFuture(channel, bindIPMap, fixedIp);
            return fixedIp;
        }
        for (Map.Entry<String, AtomicReference<Channel>> entry : bindIPMap.entrySet()) {
            if (entry.getValue().compareAndSet(null, channel)) {
                String vip = entry.getKey();
                bindCloseFuture(channel, bindIPMap, vip);
                return vip;
            }
        }
        throw new ProcessCodeException(SDWanProtos.MessageCode.NotEnough_VALUE);
    }

    private void bindCloseFuture(Channel channel, Map<String, AtomicReference<Channel>> bindIPMap, String vip) {
        channel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                bindIPMap.get(vip).set(null);
            }
        });
    }

    @Getter
    @Setter
    private static class TenantSpace {

        private List<String> stunServerList = new ArrayList<>();
        private List<String> relayServerList = new ArrayList<>();
        private Cidr ipPool;
        private Map<String, String> fixedVipMap = new ConcurrentHashMap<>();
        private List<RouteConfig> routeList = new ArrayList<>();
        private List<RouteRuleConfig> routeRuleList = new ArrayList<>();
        private List<VNATConfig> vnatList = new ArrayList<>();
        private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();
    }
}
