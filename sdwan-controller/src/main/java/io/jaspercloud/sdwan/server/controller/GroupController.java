package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.common.ValidGroup;
import io.jaspercloud.sdwan.server.controller.request.EditGroupMemberRequest;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.*;
import io.jaspercloud.sdwan.server.service.*;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group")
public class GroupController extends BaseController {

    @Resource
    private GroupService groupService;

    @Resource
    private NodeService nodeService;

    @Resource
    private RouteService routeService;

    @Resource
    private RouteRuleService routeRuleService;

    @Resource
    private VNATService vnatService;

    @PostMapping("/add")
    public void add(@Validated(ValidGroup.Add.class) @RequestBody EditGroupRequest request) {
        groupService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@Validated(ValidGroup.Update.class) @RequestBody EditGroupRequest request) {
        groupService.edit(request);
    }

    @PostMapping("/del")
    public void del(@Validated(ValidGroup.Delete.class) @RequestBody EditGroupRequest request) {
        groupService.del(request);
    }

    @GetMapping("/list")
    public List<GroupResponse> list() {
        List<GroupResponse> list = groupService.list();
        return list;
    }

    @GetMapping("/page")
    public PageResponse<GroupResponse> page() {
        PageResponse<GroupResponse> response = groupService.page();
        return response;
    }

    @PostMapping("/updateMemberList")
    public void updateMemberList(@Validated @RequestBody EditGroupMemberRequest request) {
        //member
        for (Long nodeId : request.getNodeIdList()) {
            Node node = nodeService.queryById(nodeId);
            if (null == node) {
                throw new ProcessException("not found node");
            }
        }
        //route
        for (Long routeId : request.getRouteIdList()) {
            Route route = routeService.queryById(routeId);
            if (null == route) {
                throw new ProcessException("not found route");
            }
        }
        //routeRule
        for (Long routeRuleId : request.getRouteRuleIdList()) {
            RouteRule routeRule = routeRuleService.queryById(routeRuleId);
            if (null == routeRule) {
                throw new ProcessException("not found routeRule");
            }
        }
        //vnat
        for (Long vnatId : request.getVnatIdList()) {
            VNAT vnat = vnatService.queryById(vnatId);
            if (null == vnat) {
                throw new ProcessException("not found vnat");
            }
        }
        groupService.updateMemberList(request);
        reloadClientList();
    }

    @GetMapping("/detail/{id}")
    public Group detail(@PathVariable("id") Long groupId) {
        Group group = groupService.queryDetail(groupId);
        return group;
    }
}
