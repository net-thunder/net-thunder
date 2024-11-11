package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteResponse {

    private Long id;
    private String name;
    private String description;
    private String destination;
    private List<NodeResponse> nodeList;
    private Boolean enable;
}
