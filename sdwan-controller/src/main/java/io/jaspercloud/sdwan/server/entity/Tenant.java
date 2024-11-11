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
    private String config;
    private Boolean enable;
    private Long accountId;
}
