package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.repository.AccountRepository;
import io.jaspercloud.sdwan.server.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountRepository accountRepository;

    @Override
    public Account queryAccount(String username, String password) {
        Account account = accountRepository.lambdaQueryChain()
                .eq(Account::getUsername, username)
                .eq(Account::getPassword, password)
                .one();
        return account;
    }
}
