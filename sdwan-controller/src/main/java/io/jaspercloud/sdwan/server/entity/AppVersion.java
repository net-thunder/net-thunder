package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class AppVersion extends BaseEntity {

    private String name;
    private String description;
    private String path;
    private String os;
    private Date createTime;
}
