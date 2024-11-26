package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Group;

import java.util.List;

public interface GroupService {

    Long addDefaultGroup();

    void add(EditGroupRequest request);

    void edit(EditGroupRequest request);

    void del(EditGroupRequest request);

    List<GroupResponse> list();

    PageResponse<GroupResponse> page();

    void addMember(Long groupId, Long memberId);

    void updateMemberList(Long groupId, List<Long> memberIdList);

    void delAllGroupMember(Long memberId);

    Group queryById(Long groupId);

    List<Long> memberList(Long groupId);

    List<Group> queryByMemberId(Long memberId);

    List<Long> queryGroupIdListByMemberId(Long memberId);

    List<Group> queryDetailList(List<Long> groupIdList);

    List<Group> queryByIdList(List<Long> groupIdList);

    Group queryDefaultGroup();
}
