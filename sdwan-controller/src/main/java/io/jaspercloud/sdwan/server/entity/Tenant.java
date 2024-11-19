package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Tenant extends BaseEntity {

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
    private Integer ipIndex;
    private Long accountId;
}
