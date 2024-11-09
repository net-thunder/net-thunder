package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditTenantRequest {

    private Long id;
    private String name;
    private String cidr;
    private String username;
    private String password;
    private Boolean enable;
}
