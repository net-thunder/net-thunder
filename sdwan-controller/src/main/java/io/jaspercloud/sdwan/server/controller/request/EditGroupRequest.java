package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditGroupRequest {

    private Long id;
    private String name;
    private String description;
}
