package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupRouteRule extends BaseEntity {

    private Long groupId;
    private Long ruleId;
}
