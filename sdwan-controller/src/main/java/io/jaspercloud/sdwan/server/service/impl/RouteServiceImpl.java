package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.entity.RouteNodeItem;
import io.jaspercloud.sdwan.server.repository.RouteNodeItemRepository;
import io.jaspercloud.sdwan.server.repository.RouteRepository;
import io.jaspercloud.sdwan.server.repository.po.RouteNodeItemPO;
import io.jaspercloud.sdwan.server.repository.po.RoutePO;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.RouteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteServiceImpl implements RouteService {

    @Resource
    private RouteRepository routeRepository;

    @Resource
    private RouteNodeItemRepository routeNodeItemRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void add(EditRouteRequest request) {
        RoutePO route = BeanUtil.toBean(request, RoutePO.class);
        route.setId(null);
        route.insert();
        if (CollectionUtil.isNotEmpty(request.getNodeIdList())) {
            for (Long nodeId : request.getNodeIdList()) {
                RouteNodeItemPO routeNodeItem = new RouteNodeItemPO();
                routeNodeItem.setRouteId(route.getId());
                routeNodeItem.setNodeId(nodeId);
                routeNodeItem.insert();
            }
        }
    }

    @Override
    public void edit(EditRouteRequest request) {
        Route route = BeanUtil.toBean(request, Route.class);
        routeRepository.updateById(route);
        if (CollectionUtil.isNotEmpty(request.getNodeIdList())) {
            routeNodeItemRepository.delete(routeNodeItemRepository.lambdaQuery()
                    .eq(RouteNodeItem::getRouteId, request.getId()));
            for (Long nodeId : request.getNodeIdList()) {
                RouteNodeItemPO routeNodeItem = new RouteNodeItemPO();
                routeNodeItem.setRouteId(route.getId());
                routeNodeItem.setNodeId(nodeId);
                routeNodeItem.insert();
            }
        }
    }

    @Override
    public void del(EditRouteRequest request) {
        if (groupConfigService.usedRoute(request.getId())) {
            throw new ProcessException("node used");
        }
        routeRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<Route> page() {
        Long total = routeRepository.query().count();
        List<Route> list = routeRepository.query().list();
        PageResponse<Route> response = PageResponse.build(list, total, 0L, 0L);
        return response;
    }

    @Override
    public Route queryById(Long id) {
        Route route = routeRepository.selectById(id);
        return route;
    }

    @Override
    public Route queryDetailById(Long id) {
        Route route = queryById(id);
        if (null == route) {
            return null;
        }
        List<Long> collect = routeNodeItemRepository.query()
                .select(RouteNodeItem::getNodeId)
                .eq(RouteNodeItem::getRouteId, id)
                .list()
                .stream().map(e -> e.getNodeId()).collect(Collectors.toList());
        route.setNodeIdList(collect);
        return route;
    }

    @Override
    public List<Route> queryDetailByIdList(List<Long> idList) {
        List<Route> routeList = routeRepository.selectBatchIds(idList);
        Map<Long, List<RouteNodeItem>> map = routeNodeItemRepository.query()
                .in(RouteNodeItem::getRouteId, routeList.stream().map(e -> e.getId()).collect(Collectors.toList()))
                .list()
                .stream().collect(Collectors.groupingBy(e -> e.getRouteId()));
        routeList.forEach(route -> {
            List<Long> collect = map.getOrDefault(route.getId(), Collections.emptyList())
                    .stream().map(e -> e.getNodeId()).collect(Collectors.toList());
            route.setNodeIdList(collect);
        });
        return routeList;
    }

    @Override
    public List<Route> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Route> list = routeRepository.list(routeRepository.lambdaQuery()
                .in(Route::getId, idList));
        return list;
    }

    @Override
    public boolean usedNode(Long nodeId) {
        Long count = routeNodeItemRepository.query()
                .eq(RouteNodeItem::getNodeId, nodeId)
                .count();
        return count > 0;
    }
}
