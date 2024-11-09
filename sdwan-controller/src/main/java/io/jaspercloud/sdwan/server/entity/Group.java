package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Group extends TenantEntity<VNAT> {

    private Long id;
    private String name;
}
