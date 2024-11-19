package io.jaspercloud.sdwan.server.controller.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditGroupMemberRequest {

    @NotNull
    private Long groupId;
    @NotNull
    private List<Long> memberIdList;
}
