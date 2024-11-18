package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.request.EditTenantRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.TenantResponse;
import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.enums.UserRole;
import io.jaspercloud.sdwan.server.repository.AccountRepository;
import io.jaspercloud.sdwan.server.repository.TenantRepository;
import io.jaspercloud.sdwan.server.repository.po.AccountPO;
import io.jaspercloud.sdwan.server.repository.po.TenantPO;
import io.jaspercloud.sdwan.server.service.GroupService;
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

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private GroupService groupService;

    @Override
    public void add(EditTenantRequest request) {
        Long usernameCount = accountRepository.query()
                .eq(Account::getUsername, request.getUsername())
                .count();
        if (usernameCount > 0) {
            throw new ProcessException("username exists");
        }
        AccountPO accountPO = new AccountPO();
        accountPO.setUsername(request.getUsername());
        accountPO.setPassword(request.getPassword());
        accountPO.setRole(UserRole.TenantAdmin.name());
        accountPO.insert();
        Long codeCount = tenantRepository.query()
                .eq(Tenant::getCode, request.getCode())
                .count();
        if (codeCount > 0) {
            throw new ProcessException("code exists");
        }
        TenantPO tenant = BeanUtil.toBean(request, TenantPO.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("stunServerList", request.getStunServerList());
        jsonObject.set("relayServerList", request.getRelayServerList());
        tenant.setConfig(jsonObject.toString());
        tenant.setId(null);
        tenant.setAccountId(accountPO.getId());
        tenant.insert();
        TenantContextHandler.setTenantId(tenant.getId());
        groupService.addDefaultGroup("default");
    }

    @Override
    public void edit(EditTenantRequest request) {
        Tenant tenant = tenantRepository.selectById(request.getId());
        if (null == tenant) {
            return;
        }
        Account account = accountRepository.query()
                .eq(Account::getId, tenant.getAccountId())
                .one();
        if (null != request.getPassword()) {
            account.setPassword(request.getPassword());
        }
        accountRepository.updateById(account);
        JSONObject config = JSONUtil.parseObj(tenant.getConfig());
        JSONObject jsonObject = new JSONObject();
        if (null != request.getName()) {
            tenant.setName(request.getName());
        }
        if (null != request.getDescription()) {
            tenant.setDescription(request.getDescription());
        }
        if (null != request.getEnable()) {
            tenant.setEnable(request.getEnable());
        }
        if (null != request.getNodeGrant()) {
            tenant.setNodeGrant(request.getNodeGrant());
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
    public Tenant queryById(Long id) {
        Tenant tenant = tenantRepository.selectById(id);
        return tenant;
    }

    @Override
    public List<Tenant> list() {
        List<Tenant> list = tenantRepository.query().list();
        return list;
    }

    @Override
    public PageResponse<Tenant> page() {
        Long total = tenantRepository.query().count();
        List<Tenant> list = tenantRepository.query().list();
        PageResponse<Tenant> response = PageResponse.build(list, total, 0L, 0L);
        return response;
    }

    @Override
    public TenantResponse queryByTenantCode(String tenantCode) {
        Tenant tenant = tenantRepository.query()
                .eq(Tenant::getCode, tenantCode)
                .one();
        if (null == tenant) {
            return null;
        }
        JSONObject jsonObject = JSONUtil.parseObj(tenant.getConfig());
        TenantResponse tenantResponse = BeanUtil.toBean(tenant, TenantResponse.class);
        tenantResponse.setStunServerList(jsonObject.getBeanList("stunServerList", String.class));
        tenantResponse.setRelayServerList(jsonObject.getBeanList("relayServerList", String.class));
        return tenantResponse;
    }

    @Override
    public Integer incIpIndex(Long tenantId) {
        boolean update;
        Integer index;
        do {
            Tenant tenant = tenantRepository.selectById(tenantId);
            Integer ipIndex = tenant.getIpIndex();
            index = ipIndex + 1;
            update = tenantRepository.update()
                    .eq(Tenant::getId, tenantId)
                    .eq(Tenant::getIpIndex, ipIndex)
                    .set(Tenant::getIpIndex, index)
                    .update();
        } while (!update);
        return index;
    }

    @Override
    public Tenant queryByAccountId(Long accountId) {
        Tenant tenant = tenantRepository.query()
                .eq(Tenant::getAccountId, accountId)
                .one();
        return tenant;
    }
}
