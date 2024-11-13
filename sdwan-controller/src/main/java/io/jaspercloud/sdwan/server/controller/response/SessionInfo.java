package io.jaspercloud.sdwan.server.controller.response;

import io.jaspercloud.sdwan.server.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionInfo {

    private Long accountId;
    private String username;
    private UserRole role;
    private Long tenantId;
    private String tenantCode;
}
