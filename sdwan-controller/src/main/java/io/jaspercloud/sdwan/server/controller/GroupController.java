package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.server.controller.request.EditGroupMemberRequest;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.service.GroupService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group")
public class GroupController {

    @Resource
    private GroupService groupService;

    @PostMapping("/add")
    public void add(@RequestBody EditGroupRequest request) {
        groupService.add(request);
    }

    @PostMapping("/edit")
    public void edit(@RequestBody EditGroupRequest request) {
        groupService.edit(request);
    }

    @PostMapping("/del")
    public void del(@RequestBody EditGroupRequest request) {
        groupService.del(request);
    }

    @GetMapping("/page")
    public PageResponse<GroupResponse> page() {
        PageResponse<GroupResponse> response = groupService.page();
        return response;
    }

    @PostMapping("/addMember")
    public void addMember(@RequestBody EditGroupMemberRequest request) {
        groupService.addMember(request);
    }

    @PostMapping("/delMember")
    public void delMember(@RequestBody EditGroupMemberRequest request) {
        groupService.delMember(request);
    }

    @GetMapping("/memberList")
    public List<Node> memberList(@RequestParam("groupId") Long groupId) {
        List<Node> nodeList = groupService.memberList(groupId);
        return nodeList;
    }
}
