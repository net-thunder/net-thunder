package io.jaspercloud.sdwan.server.controller.request;

import io.jaspercloud.sdwan.server.enums.DirectionEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditRouteRuleRequest {

    private Long id;
    private String name;
    private String description;
    private DirectionEnum direction;
    private List<String> ruleList;
    private Boolean enable;

    private List<Long> groupIdList;
}
