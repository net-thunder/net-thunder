package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMember extends TenantEntity<GroupMember> {

    private Long id;
    private Long groupId;
    private Long memberId;
}
