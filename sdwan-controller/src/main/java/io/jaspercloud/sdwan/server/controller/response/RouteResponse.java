package io.jaspercloud.sdwan.server.controller.response;

import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.entity.Node;
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
    private List<Node> nodeList;
    private List<Group> groupList;
    private Boolean enable;
}
