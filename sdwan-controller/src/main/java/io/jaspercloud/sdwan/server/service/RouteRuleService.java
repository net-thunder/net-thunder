package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.request.RouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.entity.RouteRule;

import java.util.List;

public interface RouteRuleService {

    void addDefaultRouteRule(Long groupId);

    void add(EditRouteRuleRequest request);

    void edit(EditRouteRuleRequest request);

    void del(EditRouteRuleRequest request);

    List<RouteRuleResponse> list(RouteRuleRequest request);

    PageResponse<RouteRuleResponse> page();

    RouteRule queryById(Long id);

    RouteRule queryDetailById(Long id);

    List<RouteRule> queryByIdList(List<Long> idList);
}
