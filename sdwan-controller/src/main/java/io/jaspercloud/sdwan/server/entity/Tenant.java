package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tenant extends BaseEntity {

    private String name;
    private String description;
    private String code;
    private String cidr;
    private String username;
    private String password;
    private String config;
    private Boolean enable;
    private Integer ipIndex;
    private Long accountId;
    private Boolean nodeGrant;
}
