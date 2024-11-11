package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditGroupMemberRequest;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.entity.GroupMember;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.repository.GroupMemberRepository;
import io.jaspercloud.sdwan.server.repository.GroupRepository;
import io.jaspercloud.sdwan.server.repository.po.GroupMemberPO;
import io.jaspercloud.sdwan.server.repository.po.GroupPO;
import io.jaspercloud.sdwan.server.service.GroupService;
import io.jaspercloud.sdwan.server.service.NodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupServiceImpl implements GroupService {

    @Resource
    private GroupRepository groupRepository;

    @Resource
    private NodeService nodeService;

    @Resource
    private GroupMemberRepository groupMemberRepository;

    @Override
    public void add(EditGroupRequest request) {
        GroupPO group = BeanUtil.toBean(request, GroupPO.class);
        group.setId(null);
        group.insert();
    }

    @Override
    public void edit(EditGroupRequest request) {
        GroupPO group = BeanUtil.toBean(request, GroupPO.class);
        group.updateById();
    }

    @Override
    public void del(EditGroupRequest request) {
        Long count = groupMemberRepository.count(groupMemberRepository.lambdaQuery()
                .eq(GroupMemberPO::getMemberId, request.getId()));
        if (count > 0) {
            throw new ProcessException("group used");
        }
        groupRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<GroupResponse> page() {
        Long total = groupRepository.count();
        List<Group> list = groupRepository.list();
        List<GroupResponse> collect = list.stream().map(e -> {
            GroupResponse groupResponse = BeanUtil.toBean(e, GroupResponse.class);
            return groupResponse;
        }).collect(Collectors.toList());
        PageResponse<GroupResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public void addMember(EditGroupMemberRequest request) {
        for (Long id : request.getMemberIdList()) {
            Node node = nodeService.queryById(id);
            if (null == node) {
                throw new ProcessException("not found node");
            }
            GroupMemberPO member = new GroupMemberPO();
            member.setGroupId(request.getGroupId());
            member.setMemberId(id);
            member.insert();
        }
    }

    @Override
    public void delMember(EditGroupMemberRequest request) {
        List<GroupMember> memberList = groupMemberRepository.list(groupMemberRepository.lambdaQuery()
                .eq(GroupMemberPO::getGroupId, request.getGroupId())
                .in(GroupMemberPO::getMemberId, request.getMemberIdList()));
        for (GroupMember member : memberList) {
            groupMemberRepository.deleteById(member);
        }
    }

    @Override
    public List<Long> memberList(Long groupId) {
        List<Long> idList = groupMemberRepository.list(groupMemberRepository.lambdaQuery()
                        .eq(GroupMemberPO::getGroupId, groupId))
                .stream().map(e -> e.getMemberId()).collect(Collectors.toList());
        return idList;
    }

    @Override
    public List<Group> queryByMemberId(Long memberId) {
        List<Long> idList = groupMemberRepository.list(groupMemberRepository.lambdaQuery()
                        .select(GroupMemberPO::getGroupId)
                        .eq(GroupMemberPO::getMemberId, memberId))
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Group> groupList = groupRepository.selectBatchIds(idList);
        return groupList;
    }
}
