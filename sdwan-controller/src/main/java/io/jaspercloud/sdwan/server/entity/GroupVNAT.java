package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupVNAT extends BaseEntity {

    private Long groupId;
    private Long vnatId;
}
