package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Route extends TenantEntity<Route> {

    private Long id;
    private String name;
    private String destination;
    private List<Long> nodeIdList;
    private Boolean enable;
}
