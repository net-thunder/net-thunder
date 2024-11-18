package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TenantResponse {

    private Long id;
    private String name;
    private String description;
    private String code;
    private String cidr;
    private String username;
    private List<String> stunServerList;
    private List<String> relayServerList;
    private Boolean enable;
    private Boolean nodeGrant;
    private Integer totalNode;
    private Integer onlineNode;
}
