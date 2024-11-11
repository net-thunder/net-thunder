package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteNodeItem extends BaseEntity {

    private Long routeId;
    private Long nodeId;
}
