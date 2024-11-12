package io.jaspercloud.sdwan.server.service;

import java.util.List;

public interface GroupConfigService {

    boolean usedRoute(Long routeId);

    boolean usedRouteRule(Long routeRuleId);

    boolean usedVNAT(Long vnatId);

    void updateGroupRoute(Long routeId, List<Long> groupIdList);

    void updateGroupRouteRule(Long routeRuleId, List<Long> groupIdList);

    void updateGroupVNAT(Long vnatId, List<Long> groupIdList);

    List<Long> queryGroupRouteList(Long routeId);

    List<Long> queryGroupRouteRuleList(Long routeRuleId);

    List<Long> queryGroupVNATList(Long vnatId);
}
