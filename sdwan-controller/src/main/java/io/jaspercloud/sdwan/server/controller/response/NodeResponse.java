package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeResponse {

    private Long id;
    private String mac;
    private String ip;
    private String vip;
    private Boolean enable;
}