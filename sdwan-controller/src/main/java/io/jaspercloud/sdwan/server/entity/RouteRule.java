package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteRule extends TenantEntity<RouteRule> {

    private Long id;
    private String name;
    private String direction;
    private List<String> ruleList;
    private Boolean enable;
}
