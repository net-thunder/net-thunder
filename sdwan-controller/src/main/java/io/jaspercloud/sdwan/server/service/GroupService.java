package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditGroupMemberRequest;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.entity.Node;

import java.util.List;

public interface GroupService {

    void add(EditGroupRequest request);

    void edit(EditGroupRequest request);

    void del(EditGroupRequest request);

    PageResponse<GroupResponse> page();

    void addMember(EditGroupMemberRequest request);

    void delMember(EditGroupMemberRequest request);

    List<Node> memberList(Long groupId);

    List<Group> queryByMemberId(Long memberId);
}
