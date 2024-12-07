package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.AccountRequest;
import io.jaspercloud.sdwan.server.entity.Account;

public interface AccountService {

    Account queryAccount(String username, String password);

    Account queryByTenantId(Long tenantId);

    void updatePassword(Long tenantId, AccountRequest request);
}
