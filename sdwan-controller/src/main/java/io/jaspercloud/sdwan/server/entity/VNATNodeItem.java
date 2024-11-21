package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VNATNodeItem extends BaseEntity {

    private Long vnatId;
    private Long nodeId;
}
