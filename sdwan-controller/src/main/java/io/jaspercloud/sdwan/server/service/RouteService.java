package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteResponse;
import io.jaspercloud.sdwan.server.entity.Route;

import java.util.List;

public interface RouteService {

    void add(EditRouteRequest request);

    void edit(EditRouteRequest request);

    void del(EditRouteRequest request);

    PageResponse<RouteResponse> page();

    Route queryById(Long id);

    Route queryDetailById(Long id);

    List<Route> queryByIdList(List<Long> idList);

    boolean usedNode(Long nodeId);
}
