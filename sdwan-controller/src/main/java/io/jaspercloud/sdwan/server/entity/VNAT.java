package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VNAT extends BaseEntity {

    private String name;
    private String description;
    private String srcCidr;
    private String dstCidr;
    private List<Long> groupIdList;
    private Boolean enable;
}
