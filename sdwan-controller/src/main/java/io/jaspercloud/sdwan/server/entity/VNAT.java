package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VNAT extends BaseEntity {

    private String name;
    private String description;
    private String srcCidr;
    private String dstCidr;
    private Boolean enable;
}
