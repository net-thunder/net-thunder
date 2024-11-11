package io.jaspercloud.sdwan.server.service;

public interface NodeConfigService {

    boolean existsNode(Long nodeId);

    boolean usedRoute(Long routeId);

    boolean usedRouteRule(Long routeRuleId);

    boolean usedVNAT(Long vnatId);
}
