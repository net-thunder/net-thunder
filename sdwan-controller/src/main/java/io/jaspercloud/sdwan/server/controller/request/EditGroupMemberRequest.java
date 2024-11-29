package io.jaspercloud.sdwan.server.controller.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditGroupMemberRequest {

    @NotNull
    private Long id;
    @NotNull
    private List<Long> nodeIdList;
    @NotNull
    private List<Long> routeIdList;
    @NotNull
    private List<Long> routeRuleIdList;
    @NotNull
    private List<Long> vnatIdList;
}
