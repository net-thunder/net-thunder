package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeRoute extends TenantEntity<NodeRoute> {

    private Long id;
    private Long nodeId;
    private Long routeId;
}
