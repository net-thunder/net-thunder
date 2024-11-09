package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VNATResponse {

    private Long id;
    private String name;
    private String srcCidr;
    private String dstCidr;
    private Boolean enable;
}
