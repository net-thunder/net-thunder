package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantResponse {

    private Long id;
    private String name;
    private String code;
    private String cidr;
    private Boolean enable;
    private Integer totalNode;
    private Integer onlineNode;
}
