package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.RouteRuleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/route-rule")
public class RouteRuleController {

    @Resource
    private RouteRuleService routeRuleService;

    @Resource
    private GroupConfigService groupConfigService;

    @PostMapping("/add")
    public void add(@RequestBody EditRouteRuleRequest request) {
        routeRuleService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditRouteRuleRequest request) {
        routeRuleService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditRouteRuleRequest request) {
        routeRuleService.del(request);
    }

    @PostMapping("/page")
    public PageResponse<RouteRuleResponse> page() {
        PageResponse<RouteRuleResponse> response = routeRuleService.page();
        return response;
    }

    @PostMapping("/updateConfigList")
    public void updateConfigList(@RequestBody EditRouteRuleRequest request) {
        Long id = request.getId();
        List<Long> groupIdList = request.getGroupIdList();
        groupConfigService.updateGroupRouteRule(id, groupIdList);
    }

    @GetMapping("/configList/{id}")
    public List<Long> configList(@PathVariable("id") Long id) {
        List<Long> list = groupConfigService.queryGroupRouteRuleList(id);
        return list;
    }
}
