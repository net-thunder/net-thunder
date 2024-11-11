package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteRuleResponse {

    private Long id;
    private String name;
    private String description;
    private String direction;
    private List<String> ruleList;
    private Boolean enable;
}
