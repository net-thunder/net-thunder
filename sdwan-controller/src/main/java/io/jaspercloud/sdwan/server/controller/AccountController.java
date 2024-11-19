package io.jaspercloud.sdwan.server.controller;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.LoginRequest;
import io.jaspercloud.sdwan.server.controller.response.LoginResponse;
import io.jaspercloud.sdwan.server.controller.response.SessionInfo;
import io.jaspercloud.sdwan.server.entity.Account;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.service.AccountService;
import io.jaspercloud.sdwan.server.service.TenantService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Resource
    private AccountService accountService;

    @Resource
    private TenantService tenantService;

    @PostMapping("/login")
    public LoginResponse login(@Validated @RequestBody LoginRequest request) {
        Account account = accountService.queryAccount(request.getUsername(), request.getPassword());
        if (null == account) {
            throw new ProcessException("账号密码错误");
        }
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setAccountId(account.getId());
        sessionInfo.setUsername(account.getUsername());
        sessionInfo.setRole(account.getRole());
        Tenant tenant = tenantService.queryByAccountId(account.getId());
        if (null != tenant) {
            sessionInfo.setTenantId(tenant.getId());
            sessionInfo.setTenantCode(tenant.getCode());
        }
        SaLoginModel model = new SaLoginModel();
        model.setTokenSignTag(sessionInfo);
        StpUtil.login(account.getId(), model);
        String tokenValue = StpUtil.getTokenValue();
        LoginResponse response = new LoginResponse();
        response.setAccessToken(tokenValue);
        if (null != tenant) {
            response.setTenantId(tenant.getId());
        }
        return response;
    }

    @GetMapping("/userInfo")
    public SessionInfo userInfo(@RequestHeader("Access-Token") String token) {
        SessionInfo sessionInfo = (SessionInfo) StpUtil.getSession().getTokenSign(token).getTag();
        return sessionInfo;
    }
}