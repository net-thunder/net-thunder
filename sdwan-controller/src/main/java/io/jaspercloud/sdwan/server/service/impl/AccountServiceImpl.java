package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.repository.AccountRepository;
import io.jaspercloud.sdwan.server.service.AccountService;
import io.jaspercloud.sdwan.server.service.TenantService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class AccountServiceImpl implements AccountService {

    @Resource
    private TenantService tenantService;

    @Resource
    private AccountRepository accountRepository;

    @Override
    public Account queryAccount(String username, String password) {
        Account account = accountRepository.query()
                .eq(Account::getUsername, username)
                .eq(Account::getPassword, password)
                .one();
        return account;
    }

    @Override
    public Account queryByTenantId(Long tenantId) {
        Tenant tenant = tenantService.queryById(tenantId);
        if (null == tenant) {
            return null;
        }
        Account account = accountRepository.query()
                .eq(Account::getId, tenant.getAccountId())
                .one();
        return account;
    }
}
