package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Node extends BaseEntity {

    private String name;
    private String description;
    private String mac;
    private String os;
    private String osVersion;
    private String vip;
    private Boolean enable;
    private List<Long> groupIdList;
    private Boolean online;
}
