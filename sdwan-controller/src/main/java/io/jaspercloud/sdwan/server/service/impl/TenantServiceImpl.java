package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
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
import io.jaspercloud.sdwan.server.service.RouteRuleService;
import io.jaspercloud.sdwan.server.service.TenantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class TenantServiceImpl implements TenantService {

    @Resource
    private TenantRepository tenantRepository;

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private GroupService groupService;

    @Resource
    private RouteRuleService routeRuleService;

    @Override
    public void add(EditTenantRequest request) {
        checkUnique(request.getId(), request.getUsername(), request.getName(), request.getCode());
        AccountPO accountPO = new AccountPO();
        accountPO.setUsername(request.getUsername());
        accountPO.setPassword(request.getPassword());
        accountPO.setRole(UserRole.TenantAdmin.name());
        accountPO.insert();
        TenantPO tenant = BeanUtil.toBean(request, TenantPO.class);
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("stunServerList", request.getStunServerList());
        jsonObject.set("relayServerList", request.getRelayServerList());
        tenant.setConfig(jsonObject.toString());
        tenant.setId(null);
        tenant.setAccountId(accountPO.getId());
        tenant.insert();
        TenantContextHandler.setTenantId(tenant.getId());
        Long groupId = groupService.addDefaultGroup();
        routeRuleService.addDefaultRouteRule(groupId);
    }

    @Override
    public void edit(EditTenantRequest request) {
        checkUnique(request.getId(), request.getUsername(), request.getName(), request.getCode());
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
        if (null != request.getName()) {
            tenant.setName(request.getName());
        }
        if (null != request.getDescription()) {
            tenant.setDescription(request.getDescription());
        }
        tenant.setStunServerList(request.getStunServerList());
        tenant.setRelayServerList(request.getRelayServerList());
        if (null != request.getEnable()) {
            tenant.setEnable(request.getEnable());
        }
        if (null != request.getNodeGrant()) {
            tenant.setNodeGrant(request.getNodeGrant());
        }
        tenantRepository.updateById(tenant);
    }

    private void checkUnique(Long tenantId, String username, String name, String code) {
        Long usernameCount = accountRepository.query()
                .eq(Account::getUsername, username)
                .func(null != tenantId, w -> {
                    Tenant tenant = tenantRepository.selectById(tenantId);
                    w.ne(Account::getId, tenant.getAccountId());
                })
                .count();
        if (usernameCount > 0) {
            throw new ProcessException("账号名已存在");
        }
        Long nameCount = tenantRepository.query()
                .eq(Tenant::getName, name)
                .func(null != tenantId, w -> {
                    w.ne(Tenant::getId, tenantId);
                })
                .count();
        if (nameCount > 0) {
            throw new ProcessException("租户名已存在");
        }
        Long codeCount = tenantRepository.query()
                .eq(Tenant::getCode, code)
                .func(null != tenantId, w -> {
                    w.ne(Tenant::getId, tenantId);
                })
                .count();
        if (codeCount > 0) {
            throw new ProcessException("租户编码已存在");
        }
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
        TenantResponse tenantResponse = BeanUtil.toBean(tenant, TenantResponse.class);
        return tenantResponse;
    }

    @Override
    public boolean updateIpIndex(Long tenantId, Integer oldIndex, Integer newIndex) {
        return tenantRepository.update()
                .eq(Tenant::getId, tenantId)
                .eq(Tenant::getIpIndex, oldIndex)
                .set(Tenant::getIpIndex, newIndex)
                .update();
    }

    @Override
    public Tenant queryByAccountId(Long accountId) {
        Tenant tenant = tenantRepository.query()
                .eq(Tenant::getAccountId, accountId)
                .one();
        return tenant;
    }
}
