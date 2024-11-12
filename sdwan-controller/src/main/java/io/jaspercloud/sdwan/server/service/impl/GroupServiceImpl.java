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
import io.jaspercloud.sdwan.server.repository.*;
import io.jaspercloud.sdwan.server.repository.po.*;
import io.jaspercloud.sdwan.server.service.GroupService;
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
    private GroupMemberRepository groupMemberRepository;

    @Resource
    private GroupRouteRepository groupRouteRepository;

    @Resource
    private GroupRouteRuleRepository groupRouteRuleRepository;

    @Resource
    private GroupVNATRepository groupVNATRepository;

    @Override
    public void addDefaultGroup(String name) {
        GroupPO group = new GroupPO();
        group.setName(name);
        group.setDefaultGroup(true);
        group.insert();
    }

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
    public void addMember(Long groupId, Long memberId) {
        Long count = groupMemberRepository.count(groupMemberRepository.lambdaQuery()
                .eq(GroupMemberPO::getGroupId, groupId)
                .in(GroupMemberPO::getMemberId, memberId));
        if (count > 0) {
            return;
        }
        GroupMemberPO groupMemberPO = new GroupMemberPO();
        groupMemberPO.setGroupId(groupId);
        groupMemberPO.setMemberId(memberId);
        groupMemberPO.insert();
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
    public Group queryById(Long groupId) {
        Group group = groupRepository.selectById(groupId);
        return group;
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

    @Override
    public List<Long> queryGroupIdListByMemberId(Long memberId) {
        List<Long> collect = groupMemberRepository.list(groupMemberRepository.lambdaQuery()
                        .select(GroupMemberPO::getGroupId)
                        .eq(GroupMemberPO::getMemberId, memberId))
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Group> queryDetailList(List<Long> groupIdList) {
        if (CollectionUtil.isEmpty(groupIdList)) {
            return Collections.emptyList();
        }
        List<Group> groupList = groupRepository.list(groupRepository.lambdaQuery()
                .in(GroupPO::getId, groupIdList));
        groupList.forEach(group -> {
            List<Long> routeIdList = groupRouteRepository.list(groupRouteRepository.lambdaQuery()
                            .eq(GroupRoutePO::getGroupId, group.getId()))
                    .stream().map(e -> e.getRouteId()).collect(Collectors.toList());
            List<Long> ruleIdList = groupRouteRuleRepository.list(groupRouteRuleRepository.lambdaQuery()
                            .eq(GroupRouteRulePO::getGroupId, group.getId()))
                    .stream().map(e -> e.getRuleId()).collect(Collectors.toList());
            List<Long> vnatIdList = groupVNATRepository.list(groupVNATRepository.lambdaQuery()
                            .eq(GroupVNATPO::getGroupId, group.getId()))
                    .stream().map(e -> e.getVnatId()).collect(Collectors.toList());
            group.setRouteIdList(routeIdList);
            group.setRouteRuleIdList(ruleIdList);
            group.setVnatIdList(vnatIdList);
        });
        return groupList;
    }

    @Override
    public Group queryDefaultGroup() {
        Group group = groupRepository.one(groupRepository.lambdaQuery()
                .eq(GroupPO::getDefaultGroup, true));
        return group;
    }

    @Override
    public void delAllGroupMember(Long memberId) {
        groupMemberRepository.delete(groupMemberRepository.lambdaQuery()
                .eq(GroupMemberPO::getMemberId, memberId));
    }
}
