package io.jaspercloud.sdwan.server.controller.response;

import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.entity.VNAT;
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
    private String vip;
    private List<Group> groupList;
    private List<Route> routeList;
    private List<RouteRule> routeRuleList;
    private List<VNAT> vnatList;
    private Boolean enable;
    private Boolean online;
    private Long tenantId;
}
