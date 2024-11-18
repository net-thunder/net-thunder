package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteResponse;
import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.service.GroupService;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.RouteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/route")
public class RouteController {

    @Resource
    private RouteService routeService;

    @Resource
    private NodeService nodeService;

    @Resource
    private GroupService groupService;

    @PostMapping("/add")
    public void add(@RequestBody EditRouteRequest request) {
        if (CollectionUtil.isNotEmpty(request.getNodeIdList())) {
            for (Long nodeId : request.getNodeIdList()) {
                boolean exists = nodeService.existsNode(nodeId);
                if (!exists) {
                    throw new ProcessException("not found node");
                }
            }
        }
        routeService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditRouteRequest request) {
        if (CollectionUtil.isNotEmpty(request.getNodeIdList())) {
            for (Long nodeId : request.getNodeIdList()) {
                boolean exists = nodeService.existsNode(nodeId);
                if (!exists) {
                    throw new ProcessException("not found node");
                }
            }
        }
        routeService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditRouteRequest request) {
        routeService.del(request);
    }

    @GetMapping("/detail/{id}")
    public RouteResponse detail(@PathVariable("id") Long id) {
        Route route = routeService.queryDetailById(id);
        if (null == route) {
            return null;
        }
        RouteResponse routeResponse = BeanUtil.toBean(route, RouteResponse.class);
        List<Node> nodeList = nodeService.queryByIdList(route.getNodeIdList());
        routeResponse.setNodeList(nodeList);
        List<Group> groupList = groupService.queryByIdList(route.getGroupIdList());
        routeResponse.setGroupList(groupList);
        return routeResponse;
    }

    @GetMapping("/list")
    public List<RouteResponse> list() {
        List<Route> list = routeService.list();
        List<RouteResponse> collect = list.stream().map(e -> {
            RouteResponse routeResponse = BeanUtil.toBean(e, RouteResponse.class);
            List<Node> nodeList = nodeService.queryByIdList(e.getNodeIdList());
            routeResponse.setNodeList(nodeList);
            List<Group> groupList = groupService.queryByIdList(e.getGroupIdList());
            routeResponse.setGroupList(groupList);
            return routeResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @GetMapping("/page")
    public PageResponse<RouteResponse> page() {
        PageResponse<Route> response = routeService.page();
        List<RouteResponse> collect = response.getData().stream().map(e -> {
            RouteResponse routeResponse = BeanUtil.toBean(e, RouteResponse.class);
            return routeResponse;
        }).collect(Collectors.toList());
        PageResponse<RouteResponse> pageResponse = PageResponse.build(collect, response.getTotal(), response.getSize(), response.getCurrent());
        return pageResponse;
    }
}
