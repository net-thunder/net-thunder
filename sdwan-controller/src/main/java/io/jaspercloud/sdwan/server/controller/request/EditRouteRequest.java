package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditRouteRequest {

    private Long id;
    private String name;
    private String description;
    private String destination;
    private List<Long> nodeIdList;
    private Boolean enable;
}
