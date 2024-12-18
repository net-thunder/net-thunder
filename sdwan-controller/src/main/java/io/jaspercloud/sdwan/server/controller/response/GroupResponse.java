package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean defaultGroup;
    private Long nodeCount;
    private Long routeCount;
    private Long vnatCount;
    private Long ruleCount;
}
