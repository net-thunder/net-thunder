package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditNodeRequest {

    private Long id;
    private String name;
    private String description;
    private String mac;
    private List<Long> groupIdList;
    private Boolean enable;
}
