package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Route;

import java.util.List;

public interface RouteService {

    void add(EditRouteRequest request);

    void edit(EditRouteRequest request);

    void del(EditRouteRequest request);

    List<Route> list();

    PageResponse<Route> page();

    Route queryById(Long id);

    List<Route> queryByIdList(List<Long> idList);

    Route queryDetailById(Long id);

    List<Route> queryDetailByIdList(List<Long> idList);

    boolean usedNode(Long nodeId);
}
