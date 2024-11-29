package io.jaspercloud.sdwan.tranport.service;

import io.jaspercloud.sdwan.tranport.config.NodeConfig;
import io.jaspercloud.sdwan.tranport.config.TenantConfig;
import io.netty.channel.Channel;

import java.util.List;

public interface SdWanDataService {

    boolean hasTenant(String tenantCode);

    TenantConfig getTenantConfig(String tenantCode);

    NodeConfig assignNodeInfo(Channel channel, String tenantCode, String macAddress);

    List<String> getStunServerList(String tenantCode);

    List<String> getRelayServerList(String tenantCode);
}
