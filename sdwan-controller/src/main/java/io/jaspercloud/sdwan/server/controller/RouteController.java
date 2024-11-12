package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteResponse;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
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
    private GroupConfigService groupConfigService;

    @Resource
    private NodeService nodeService;

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

    @GetMapping("/page")
    public PageResponse<RouteResponse> page() {
        PageResponse<Route> response = routeService.page();
        List<RouteResponse> collect = response.getData().stream().map(e -> {
            RouteResponse routeResponse = BeanUtil.toBean(e, RouteResponse.class);
            if (CollectionUtil.isNotEmpty(e.getNodeIdList())) {
                List<NodeResponse> nodeList = nodeService.queryByIdList(e.getNodeIdList())
                        .stream().map(node -> BeanUtil.toBean(node, NodeResponse.class))
                        .collect(Collectors.toList());
                routeResponse.setNodeList(nodeList);
            }
            return routeResponse;
        }).collect(Collectors.toList());
        PageResponse<RouteResponse> pageResponse = PageResponse.build(collect, response.getTotal(), response.getSize(), response.getCurrent());
        return pageResponse;
    }

    @PostMapping("/updateConfigList")
    public void updateConfigList(@RequestBody EditRouteRequest request) {
        Long id = request.getId();
        List<Long> groupIdList = request.getGroupIdList();
        Route route = routeService.queryById(id);
        if (null == route) {
            throw new ProcessException("not found route");
        }
        groupConfigService.updateGroupRoute(id, groupIdList);
    }

    @GetMapping("/configList/{id}")
    public List<Long> configList(@PathVariable("id") Long id) {
        List<Long> list = groupConfigService.queryGroupRouteList(id);
        return list;
    }
}
