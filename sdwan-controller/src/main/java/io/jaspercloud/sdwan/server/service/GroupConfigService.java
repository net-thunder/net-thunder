package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.entity.GroupRoute;

import java.util.List;

public interface GroupConfigService {

    void deleteGroupRoute(Long routeId);

    void deleteGroupRouteRule(Long routeRuleId);

    void deleteGroupVNAT(Long vnatId);

    void updateGroupRoute(Long routeId, List<Long> groupIdList);

    void updateGroupRouteRule(Long routeRuleId, List<Long> groupIdList);

    void updateGroupVNAT(Long vnatId, List<Long> groupIdList);

    List<Long> queryGroupRouteList(Long routeId);

    List<GroupRoute> queryGroupRouteList(List<Long> routeIdList);

    List<Long> queryGroupRouteRuleList(Long routeRuleId);

    List<Long> queryGroupVNATList(Long vnatId);
}
