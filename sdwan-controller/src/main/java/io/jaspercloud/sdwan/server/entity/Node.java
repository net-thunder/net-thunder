package io.jaspercloud.sdwan.server.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Node extends BaseEntity {

    private String mac;
    private String name;
    private String description;
    private String ip;
    private String vip;
    private Boolean enable;
    private List<NodeRoute> nodeRouteList;
    private List<NodeRouteRule> nodeRouteRuleList;
    private List<NodeVNAT> nodeVNATList;
}
