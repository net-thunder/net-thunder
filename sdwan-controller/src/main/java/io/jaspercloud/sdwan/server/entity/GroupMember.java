package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMember extends BaseEntity {

    private Long groupId;
    private Long memberId;
}
