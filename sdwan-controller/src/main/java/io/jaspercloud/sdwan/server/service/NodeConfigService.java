package io.jaspercloud.sdwan.server.service;

public interface NodeConfigService {

    boolean usedRoute(Long routeId);

    boolean usedRouteRule(Long routeRuleId);

    boolean usedVNAT(Long vnatId);
}
