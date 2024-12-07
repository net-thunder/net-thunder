package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.AccountRequest;
import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.repository.AccountRepository;
import io.jaspercloud.sdwan.server.service.AccountService;
import io.jaspercloud.sdwan.server.service.TenantService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
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

    @Override
    public void updatePassword(Long accountId, AccountRequest request) {
        Account account = accountRepository.query()
                .eq(Account::getId, accountId)
                .one();
        if (null == account) {
            throw new ProcessException("账号不存在");
        }
        if (!StringUtils.equals(request.getPassword(), account.getPassword())) {
            throw new ProcessException("密码错误");
        }
        account.setPassword(request.getNewPassword());
        accountRepository.updateById(account);
    }
}
