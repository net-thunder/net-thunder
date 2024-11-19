package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Tenant;

import java.util.List;

public interface TenantService {

    void add(EditTenantRequest request);

    void edit(EditTenantRequest request);

    void del(EditTenantRequest request);

    boolean updateIpIndex(Long tenantId, Integer oldIndex, Integer newIndex);

    Tenant queryById(Long id);

    List<Tenant> list();

    PageResponse<Tenant> page();

    TenantResponse queryByTenantCode(String tenantCode);

    Tenant queryByAccountId(Long accountId);
}
