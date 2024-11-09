package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditRouteRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteResponse;
import io.jaspercloud.sdwan.server.service.RouteService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/route")
public class RouteController {

    @Resource
    private RouteService routeService;

    @PostMapping("/add")
    public void add(@RequestBody EditRouteRequest request) {
        routeService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditRouteRequest request) {
        routeService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditRouteRequest request) {
        routeService.del(request);
    }

    @GetMapping("/page")
    public PageResponse<RouteResponse> page() {
        PageResponse<RouteResponse> response = routeService.page();
        return response;
    }
}
