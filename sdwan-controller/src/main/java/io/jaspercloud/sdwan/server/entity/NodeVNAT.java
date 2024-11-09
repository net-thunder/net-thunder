package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeVNAT extends TenantEntity<NodeVNAT> {

    private Long id;
    private Long nodeId;
    private Long vnatId;
}
