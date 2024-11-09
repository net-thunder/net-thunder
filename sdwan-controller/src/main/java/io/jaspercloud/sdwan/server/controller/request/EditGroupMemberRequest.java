package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditGroupMemberRequest {

    private Long groupId;
    private List<Long> memberIdList;
}
