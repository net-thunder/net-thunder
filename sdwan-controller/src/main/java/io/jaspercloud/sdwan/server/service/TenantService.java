package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Tenant;

public interface TenantService {

    void add(EditTenantRequest request);

    void edit(EditTenantRequest request);

    void del(EditTenantRequest request);

    PageResponse<Tenant> page();

    TenantResponse queryByTenantCode(String tenantCode);
}
