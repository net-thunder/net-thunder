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
@Transactional(rollbackFor = Exception.class)
public class RouteServiceImpl implements RouteService {

    @Resource
    private RouteRepository routeRepository;

    @Resource
    private RouteNodeItemRepository routeNodeItemRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void add(EditRouteRequest request) {
        checkUnique(request.getId(), request.getName());
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
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            groupConfigService.updateGroupRoute(route.getId(), request.getGroupIdList());
        }
    }

    @Override
    public void edit(EditRouteRequest request) {
        checkUnique(request.getId(), request.getName());
        Route route = BeanUtil.toBean(request, Route.class);
        routeRepository.updateById(route);
        if (null != request.getNodeIdList()) {
            routeNodeItemRepository.delete()
                    .eq(RouteNodeItem::getRouteId, request.getId())
                    .delete();
            for (Long nodeId : request.getNodeIdList()) {
                RouteNodeItemPO routeNodeItem = new RouteNodeItemPO();
                routeNodeItem.setRouteId(route.getId());
                routeNodeItem.setNodeId(nodeId);
                routeNodeItem.insert();
            }
        }
        if (null != request.getGroupIdList()) {
            groupConfigService.updateGroupRoute(route.getId(), request.getGroupIdList());
        }
    }

    private void checkUnique(Long id, String name) {
        Long count = routeRepository.query()
                .eq(Route::getName, name)
                .func(null != id, w -> {
                    w.ne(Route::getId, id);
                })
                .count();
        if (count > 0) {
            throw new ProcessException("名称已存在");
        }
    }

    @Override
    public void del(EditRouteRequest request) {
        groupConfigService.deleteGroupRoute(request.getId());
        routeRepository.deleteById(request.getId());
    }

    @Override
    public List<Route> list() {
        List<Route> list = routeRepository.query().list();
        Map<Long, List<RouteNodeItem>> routeItemMap;
        Map<Long, List<Long>> groupMap;
        if (CollectionUtil.isNotEmpty(list)) {
            routeItemMap = routeNodeItemRepository.query()
                    .in(RouteNodeItem::getRouteId, list.stream().map(e -> e.getId()).collect(Collectors.toList()))
                    .list()
                    .stream().collect(Collectors.groupingBy(RouteNodeItem::getRouteId));
            groupMap = groupConfigService.queryGroupRouteList(list.stream().map(e -> e.getId()).collect(Collectors.toList()))
                    .stream()
                    .collect(Collectors.groupingBy(e -> e.getRouteId(), Collectors.mapping(e -> e.getGroupId(), Collectors.toList())));
        } else {
            routeItemMap = Collections.emptyMap();
            groupMap = Collections.emptyMap();
        }
        list.forEach(route -> {
            List<RouteNodeItem> nodeList = routeItemMap.getOrDefault(route.getId(), Collections.emptyList());
            route.setNodeIdList(nodeList.stream().map(e -> e.getNodeId()).collect(Collectors.toList()));
            List<Long> groupIdList = groupMap.getOrDefault(route.getId(), Collections.emptyList());
            route.setGroupIdList(groupIdList);
        });
        return list;
    }

    @Override
    public PageResponse<Route> page() {
        Long total = routeRepository.query().count();
        List<Route> list = list();
        PageResponse<Route> response = PageResponse.build(list, total, 0L, 0L);
        return response;
    }

    @Override
    public Route queryById(Long id) {
        Route route = routeRepository.selectById(id);
        return route;
    }

    @Override
    public List<Route> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Route> list = routeRepository.query()
                .in(Route::getId, idList)
                .list();
        return list;
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
        List<Long> groupIdList = groupConfigService.queryGroupRouteList(route.getId());
        route.setGroupIdList(groupIdList);
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
    public boolean usedNode(Long nodeId) {
        Long count = routeNodeItemRepository.query()
                .eq(RouteNodeItem::getNodeId, nodeId)
                .count();
        return count > 0;
    }
}
