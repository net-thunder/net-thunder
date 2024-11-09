package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditRouteRuleRequest {

    private Long id;
    private String name;
    private String direction;
    private List<String> ruleList;
    private Boolean enable;
}
