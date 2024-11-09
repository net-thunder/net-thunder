package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VNAT extends TenantEntity<VNAT> {

    private Long id;
    private String name;
    private String srcCidr;
    private String dstCidr;
    private Boolean enable;
}
