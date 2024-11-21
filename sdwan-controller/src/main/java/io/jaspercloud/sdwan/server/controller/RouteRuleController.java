package io.jaspercloud.sdwan.server.controller;

import cn.hutool.core.bean.BeanUtil;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.service.GroupService;
import io.jaspercloud.sdwan.server.service.RouteRuleService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route-rule")
public class RouteRuleController extends BaseController {

    @Resource
    private RouteRuleService routeRuleService;

    @Resource
    private GroupService groupService;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditRouteRuleRequest request) {
        request.check();
        routeRuleService.add(request);
        reloadClientList();
    }

    @PostMapping("/edit")
    public void edit(@Validated(ValidGroup.Update.class) @RequestBody EditRouteRuleRequest request) {
        request.check();
        routeRuleService.edit(request);
        reloadClientList();
    }

    @PostMapping("/del")
    public void del(@Validated(ValidGroup.Delete.class) @RequestBody EditRouteRuleRequest request) {
        routeRuleService.del(request);
    }

    @GetMapping("/detail/{id}")
    public RouteRuleResponse detail(@PathVariable("id") Long id) {
        RouteRule routeRule = routeRuleService.queryDetailById(id);
        if (null == routeRule) {
            return null;
        }
        RouteRuleResponse response = BeanUtil.toBean(routeRule, RouteRuleResponse.class);
        response.setGroupIdList(routeRule.getGroupIdList());
        return response;
    }

    @GetMapping("/list")
    public List<RouteRuleResponse> list() {
        List<RouteRuleResponse> list = routeRuleService.list();
        return list;
    }

    @GetMapping("/page")
    public PageResponse<RouteRuleResponse> page() {
        PageResponse<RouteRuleResponse> response = routeRuleService.page();
        return response;
    }
}
