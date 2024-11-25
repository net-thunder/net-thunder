package io.jaspercloud.sdwan.server.controller.response;

import io.jaspercloud.sdwan.server.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NodeDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String mac;
    private String os;
    private String osVersion;
    private List<ICEAddress> addressList;
    private String ip;
    private String vip;
    private List<Group> groupList;
    private List<Route> routeList;
    private List<RouteRule> routeRuleList;
    private List<VNAT> vnatList;
    private List<Node> nodeList;
    private Boolean enable;
    private Boolean online;
    private Long tenantId;
}
