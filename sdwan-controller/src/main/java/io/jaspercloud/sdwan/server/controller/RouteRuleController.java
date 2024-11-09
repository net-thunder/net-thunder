package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.service.RouteRuleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/route-rule")
public class RouteRuleController {

    @Resource
    private RouteRuleService routeRuleService;

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
}
