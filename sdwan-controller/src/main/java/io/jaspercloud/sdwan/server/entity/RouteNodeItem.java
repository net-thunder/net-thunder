package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteNodeItem extends TenantEntity<RouteNodeItem> {

    private Long id;
    private Long routeId;
    private Long nodeId;
}
