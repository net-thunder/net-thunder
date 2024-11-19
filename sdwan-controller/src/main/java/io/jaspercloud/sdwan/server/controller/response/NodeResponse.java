package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeResponse {

    private Long id;
    private String name;
    private String description;
    private String mac;
    private String os;
    private String osVersion;
    private String ip;
    private String vip;
    private Boolean enable;
    private Boolean online;
    private Long tenantId;
}
