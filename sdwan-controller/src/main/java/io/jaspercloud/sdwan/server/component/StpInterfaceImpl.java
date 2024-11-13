package io.jaspercloud.sdwan.server.component;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import io.jaspercloud.sdwan.server.controller.response.SessionInfo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        String token = StpUtil.getTokenValue();
        SessionInfo sessionInfo = (SessionInfo) StpUtil.getSession().getTokenSign(token).getTag();
        String role = sessionInfo.getRole().name();
        return Arrays.asList(role);
    }
}
