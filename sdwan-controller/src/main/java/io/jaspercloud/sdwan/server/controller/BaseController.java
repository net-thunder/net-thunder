package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.TenantService;
import io.jaspercloud.sdwan.tranport.ControllerServer;
import jakarta.annotation.Resource;

public class BaseController {

    @Resource
    private ControllerServer controllerServer;

    @Resource
    private TenantService tenantService;

    public void reloadClient(String vip) {
        Tenant tenant = tenantService.queryById(TenantContextHandler.getCurrentTenantId());
        if (null == tenant) {
            return;
        }
        controllerServer.offlineChannel(tenant.getCode(), vip);
    }

    public void reloadClientList() {
        Tenant tenant = tenantService.queryById(TenantContextHandler.getCurrentTenantId());
        if (null == tenant) {
            return;
        }
        controllerServer.offlineChannelList(tenant.getCode());
    }
}
