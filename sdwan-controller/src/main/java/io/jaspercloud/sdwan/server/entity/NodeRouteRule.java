package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeRouteRule extends TenantEntity<NodeRouteRule> {

    private Long id;
    private Long nodeId;
    private Long ruleId;
}
