package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.repository.TenantRepository;
import io.jaspercloud.sdwan.server.repository.po.TenantPO;
import io.jaspercloud.sdwan.server.service.TenantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantRepository tenantRepository;

    @Override
    public void add(EditTenantRequest request) {
        Long codeCount = tenantRepository.count(tenantRepository.lambdaQuery()
                .eq(TenantPO::getCode, request.getCode()));
        if (codeCount > 0) {
            throw new ProcessException("code exists");
        }
        Long usernameCount = tenantRepository.count(tenantRepository.lambdaQuery()
                .eq(TenantPO::getUsername, request.getUsername()));
        if (usernameCount > 0) {
            throw new ProcessException("username exists");
        }
        TenantPO tenant = BeanUtil.toBean(request, TenantPO.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("stunServerList", request.getStunServerList());
        jsonObject.set("relayServerList", request.getRelayServerList());
        tenant.setConfig(jsonObject.toString());
        tenant.setId(null);
        tenant.insert();
    }

    @Override
    public void edit(EditTenantRequest request) {
        Tenant tenant = tenantRepository.selectById(request.getId());
        if (null == tenant) {
            return;
        }
        JSONObject config = JSONUtil.parseObj(tenant.getConfig());
        JSONObject jsonObject = new JSONObject();
        if (null != request.getName()) {
            tenant.setName(request.getName());
        }
        if (null != request.getDescription()) {
            tenant.setDescription(request.getDescription());
        }
        if (null != request.getPassword()) {
            tenant.setPassword(request.getPassword());
        }
        if (null != request.getStunServerList()) {
            jsonObject.set("stunServerList", request.getStunServerList());
        } else {
            jsonObject.set("stunServerList", config.getJSONArray("stunServerList"));
        }
        if (null != request.getRelayServerList()) {
            jsonObject.set("relayServerList", request.getRelayServerList());
        } else {
            jsonObject.set("relayServerList", config.getJSONArray("relayServerList"));
        }
        tenant.setConfig(jsonObject.toString());
        tenantRepository.updateById(tenant);
    }

    @Override
    public void del(EditTenantRequest request) {
        tenantRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<Tenant> page() {
        Long total = tenantRepository.count();
        List<Tenant> list = tenantRepository.list();
        PageResponse<Tenant> response = PageResponse.build(list, total, 0L, 0L);
        return response;
    }

    @Override
    public TenantResponse queryByTenantCode(String tenantCode) {
        Tenant tenant = tenantRepository.one(tenantRepository.lambdaQuery()
                .eq(TenantPO::getCode, tenantCode));
        if (null == tenant) {
            return null;
        }
        JSONObject jsonObject = JSONUtil.parseObj(tenant.getConfig());
        TenantResponse tenantResponse = BeanUtil.toBean(tenant, TenantResponse.class);
        tenantResponse.setStunServerList(jsonObject.getBeanList("stunServerList", String.class));
        tenantResponse.setRelayServerList(jsonObject.getBeanList("relayServerList", String.class));
        return tenantResponse;
    }
}
