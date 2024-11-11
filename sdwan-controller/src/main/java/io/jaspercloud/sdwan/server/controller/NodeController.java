package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.*;
import io.jaspercloud.sdwan.server.service.*;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/node")
public class NodeController {

    @Resource
    private NodeService nodeService;

    @Resource
    private RouteService routeService;

    @Resource
    private RouteRuleService routeRuleService;

    @Resource
    private VNATService vnatService;

    @Resource
    private GroupService groupService;

    @PostMapping("/add")
    public void add(@RequestBody EditNodeRequest request) {
        nodeService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditNodeRequest request) {
        nodeService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditNodeRequest request) {
        nodeService.del(request);
    }

    @GetMapping("/detail/{id}")
    public NodeDetailResponse detail(@PathVariable("id") Long id) {
        Node node = nodeService.queryById(id);
        NodeDetailResponse nodeResponse = BeanUtil.toBean(node, NodeDetailResponse.class);
        List<Route> routeList = routeService.queryByIdList(node.getNodeRouteList()
                .stream().map(e -> e.getRouteId()).collect(Collectors.toList()));
        nodeResponse.setRouteList(routeList);
        List<RouteRule> routeRuleList = routeRuleService.queryByIdList(node.getNodeRouteRuleList()
                .stream().map(e -> e.getRuleId()).collect(Collectors.toList()));
        nodeResponse.setRouteRuleList(routeRuleList);
        List<VNAT> vnatList = vnatService.queryIdList(node.getNodeVNATList()
                .stream().map(e -> e.getVnatId()).collect(Collectors.toList()));
        nodeResponse.setVnatList(vnatList);
        List<Group> groupList = groupService.queryByMemberId(id);
        nodeResponse.setGroupList(groupList);
        return nodeResponse;
    }

    @GetMapping("/page")
    public PageResponse<NodeResponse> page() {
        PageResponse<NodeResponse> response = nodeService.page();
        return response;
    }
}
