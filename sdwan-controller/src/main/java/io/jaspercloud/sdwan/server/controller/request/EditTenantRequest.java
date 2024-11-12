package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditTenantRequest {

    private Long id;
    private String name;
    private String description;
    private String code;
    private String cidr;
    private String username;
    private String password;
    private List<String> stunServerList;
    private List<String> relayServerList;
    private Boolean enable;
    private Boolean nodeGrant;
}
