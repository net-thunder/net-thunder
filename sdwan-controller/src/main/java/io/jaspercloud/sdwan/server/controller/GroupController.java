package io.jaspercloud.sdwan.server.controller;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditGroupMemberRequest;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.service.GroupService;
import io.jaspercloud.sdwan.server.service.NodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/group")
public class GroupController {

    @Resource
    private GroupService groupService;

    @Resource
    private NodeService nodeService;

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
    public void updateMemberList(@RequestBody EditGroupMemberRequest request) {
        for (Long id : request.getMemberIdList()) {
            Node node = nodeService.queryById(id);
            if (null == node) {
                throw new ProcessException("not found node");
            }
        }
        groupService.updateMemberList(request.getGroupId(), request.getMemberIdList());
    }

    @GetMapping("/memberList/{id}")
    public List<Node> memberList(@PathVariable("id") Long groupId) {
        List<Long> memberList = groupService.memberList(groupId);
        List<Node> nodeList = nodeService.queryByIdList(memberList);
        return nodeList;
    }
}
