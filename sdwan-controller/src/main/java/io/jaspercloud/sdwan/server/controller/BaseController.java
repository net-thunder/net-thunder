package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.tranport.SdWanServer;
import jakarta.annotation.Resource;

public class BaseController {

    @Resource
    private SdWanServer sdWanServer;

    @Resource
    private TenantService tenantService;

    public void reloadClient(String vip) {
        Tenant tenant = tenantService.queryById(TenantContextHandler.getCurrentTenantId());
        if (null == tenant) {
            return;
        }
        sdWanServer.offlineChannel(tenant.getCode(), vip);
    }

    public void reloadClientList() {
        Tenant tenant = tenantService.queryById(TenantContextHandler.getCurrentTenantId());
        if (null == tenant) {
            return;
        }
        sdWanServer.offlineChannelList(tenant.getCode());
    }
}
