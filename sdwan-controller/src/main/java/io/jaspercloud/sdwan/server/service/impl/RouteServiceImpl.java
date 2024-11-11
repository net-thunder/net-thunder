package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteResponse;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.repository.RouteNodeItemRepository;
import io.jaspercloud.sdwan.server.repository.RouteRepository;
import io.jaspercloud.sdwan.server.repository.po.RouteNodeItemPO;
import io.jaspercloud.sdwan.server.repository.po.RoutePO;
import io.jaspercloud.sdwan.server.service.NodeConfigService;
import io.jaspercloud.sdwan.server.service.RouteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteServiceImpl implements RouteService {

    @Resource
    private RouteRepository routeRepository;

    @Resource
    private RouteNodeItemRepository routeNodeItemRepository;

    @Resource
    private NodeConfigService nodeConfigService;

    @Override
    public void add(EditRouteRequest request) {
        RoutePO route = BeanUtil.toBean(request, RoutePO.class);
        route.setId(null);
        route.insert();
        for (Long nodeId : request.getNodeIdList()) {
            RouteNodeItemPO routeNodeItem = new RouteNodeItemPO();
            routeNodeItem.setRouteId(route.getId());
            routeNodeItem.setNodeId(nodeId);
            routeNodeItem.insert();
        }
    }

    @Override
    public void edit(EditRouteRequest request) {
        Route route = BeanUtil.toBean(request, Route.class);
        routeNodeItemRepository.delete(routeNodeItemRepository.lambdaQuery()
                .eq(RouteNodeItemPO::getRouteId, request.getId()));
        for (Long nodeId : request.getNodeIdList()) {
            RouteNodeItemPO routeNodeItem = new RouteNodeItemPO();
            routeNodeItem.setRouteId(route.getId());
            routeNodeItem.setNodeId(nodeId);
            routeNodeItem.insert();
        }
        routeRepository.updateById(route);
    }

    @Override
    public void del(EditRouteRequest request) {
        if (nodeConfigService.usedRoute(request.getId())) {
            throw new ProcessException("node used");
        }
        routeRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<RouteResponse> page() {
        Long total = routeRepository.count();
        List<Route> list = routeRepository.list();
        List<RouteResponse> collect = list.stream().map(e -> {
            RouteResponse routeResponse = BeanUtil.toBean(e, RouteResponse.class);
            return routeResponse;
        }).collect(Collectors.toList());
        PageResponse<RouteResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public List<Route> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Route> list = routeRepository.list(routeRepository.lambdaQuery()
                .in(RoutePO::getId, idList));
        return list;
    }

    @Override
    public boolean usedNode(Long nodeId) {
        Long count = routeNodeItemRepository.count(routeNodeItemRepository.lambdaQuery()
                .eq(RouteNodeItemPO::getNodeId, nodeId));
        return count > 0;
    }
}
