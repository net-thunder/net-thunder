package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.repository.AccountRepository;
import io.jaspercloud.sdwan.server.repository.po.AccountPO;
import io.jaspercloud.sdwan.server.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountRepository accountRepository;

    @Override
    public Account queryAccount(String username, String password) {
        Account account = accountRepository.one(accountRepository.lambdaQuery()
                .eq(AccountPO::getUsername, username)
                .eq(AccountPO::getPassword, password));
        return account;
    }
}
