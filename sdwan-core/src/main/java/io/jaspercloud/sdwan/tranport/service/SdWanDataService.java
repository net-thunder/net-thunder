package io.jaspercloud.sdwan.tranport.service;

import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.SdWanServerConfig;

import java.util.List;

public interface SdWanDataService {

    Cidr getIpPool(String tenantId);

    String applyVip(String tenantId);

    List<SdWanServerConfig.Route> getRouteList(String tenantId, String vip);

    List<SdWanServerConfig.VNAT> getVNATList(String tenantId, String vip);

    List<String> getStunServerList(String tenantId);

    List<String> getRelayServerList(String tenantId);
}
