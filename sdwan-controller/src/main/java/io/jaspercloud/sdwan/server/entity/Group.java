package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Group extends BaseEntity {

    private String name;
    private String description;
    private Boolean defaultGroup;
    private List<Long> nodeIdList;
    private List<Long> routeIdList;
    private List<Long> routeRuleIdList;
    private List<Long> vnatIdList;
}
