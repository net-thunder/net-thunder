package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteResponse;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.entity.RouteNodeItem;
import io.jaspercloud.sdwan.server.repsitory.RouteMapper;
import io.jaspercloud.sdwan.server.repsitory.RouteNodeItemMapper;
import io.jaspercloud.sdwan.server.service.NodeService;
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
    private RouteMapper routeMapper;

    @Resource
    private RouteNodeItemMapper routeNodeItemMapper;

    @Resource
    private NodeService nodeService;

    @Override
    public void add(EditRouteRequest request) {
        Route route = BeanUtil.toBean(request, Route.class);
        route.setId(null);
        route.insert();
        for (Long nodeId : request.getNodeIdList()) {
            RouteNodeItem routeNodeItem = new RouteNodeItem();
            routeNodeItem.setRouteId(route.getId());
            routeNodeItem.setNodeId(nodeId);
            routeNodeItem.insert();
        }
    }

    @Override
    public void edit(EditRouteRequest request) {
        Route route = BeanUtil.toBean(request, Route.class);
        routeNodeItemMapper.delete(Wrappers.<RouteNodeItem>lambdaQuery()
                .eq(RouteNodeItem::getRouteId, request.getId()));
        for (Long nodeId : request.getNodeIdList()) {
            RouteNodeItem routeNodeItem = new RouteNodeItem();
            routeNodeItem.setRouteId(route.getId());
            routeNodeItem.setNodeId(nodeId);
            routeNodeItem.insert();
        }
        route.updateById();
    }

    @Override
    public void del(EditRouteRequest request) {
        if (nodeService.usedRoute(request.getId())) {
            throw new ProcessException("node used");
        }
        routeMapper.deleteById(request.getId());
    }

    @Override
    public PageResponse<RouteResponse> page() {
        Long total = new LambdaQueryChainWrapper<>(routeMapper).count();
        List<Route> list = new LambdaQueryChainWrapper<>(routeMapper)
                .list();
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
        List<Route> list = new LambdaQueryChainWrapper<>(routeMapper)
                .in(Route::getId, idList)
                .list();
        return list;
    }

    @Override
    public boolean usedNode(Long nodeId) {
        Long count = new LambdaQueryChainWrapper<>(routeNodeItemMapper)
                .eq(RouteNodeItem::getNodeId, nodeId)
                .count();
        return count > 0;
    }
}
