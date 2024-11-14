package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.*;
import io.jaspercloud.sdwan.server.repository.*;
import io.jaspercloud.sdwan.server.repository.po.GroupMemberPO;
import io.jaspercloud.sdwan.server.repository.po.GroupPO;
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
        group.setDefaultGroup(false);
        group.insert();
    }

    @Override
    public void edit(EditGroupRequest request) {
        GroupPO group = BeanUtil.toBean(request, GroupPO.class);
        group.updateById();
    }

    @Override
    public void del(EditGroupRequest request) {
        Long count = groupMemberRepository.lambdaQueryChain()
                .eq(GroupMember::getMemberId, request.getId())
                .count();
        if (count > 0) {
            throw new ProcessException("group used");
        }
        groupRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<GroupResponse> page() {
        Long total = groupRepository.lambdaQueryChain().count();
        List<Group> list = groupRepository.lambdaQueryChain().list();
        List<GroupResponse> collect = list.stream().map(e -> {
            GroupResponse groupResponse = BeanUtil.toBean(e, GroupResponse.class);
            return groupResponse;
        }).collect(Collectors.toList());
        PageResponse<GroupResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public void addMember(Long groupId, Long memberId) {
        Long count = groupMemberRepository.lambdaQueryChain()
                .eq(GroupMember::getGroupId, groupId)
                .in(GroupMember::getMemberId, memberId)
                .count();
        if (count > 0) {
            return;
        }
        GroupMemberPO groupMemberPO = new GroupMemberPO();
        groupMemberPO.setGroupId(groupId);
        groupMemberPO.setMemberId(memberId);
        groupMemberPO.insert();
    }

    @Override
    public void updateMemberList(Long groupId, List<Long> memberIdList) {
        groupMemberRepository.delete(groupMemberRepository.lambdaQuery()
                .eq(GroupMember::getGroupId, groupId));
        for (Long memberId : memberIdList) {
            GroupMemberPO groupMemberPO = new GroupMemberPO();
            groupMemberPO.setGroupId(groupId);
            groupMemberPO.setMemberId(memberId);
            groupMemberPO.insert();
        }
    }

    @Override
    public Group queryById(Long groupId) {
        Group group = groupRepository.selectById(groupId);
        return group;
    }

    @Override
    public List<Long> memberList(Long groupId) {
        List<Long> idList = groupMemberRepository.lambdaQueryChain()
                .eq(GroupMember::getGroupId, groupId)
                .list()
                .stream().map(e -> e.getMemberId()).collect(Collectors.toList());
        return idList;
    }

    @Override
    public List<Group> queryByMemberId(Long memberId) {
        List<Long> idList = groupMemberRepository.lambdaQueryChain()
                .select(GroupMember::getGroupId)
                .eq(GroupMember::getMemberId, memberId)
                .list()
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Group> groupList = groupRepository.selectBatchIds(idList);
        return groupList;
    }

    @Override
    public List<Long> queryGroupIdListByMemberId(Long memberId) {
        List<Long> collect = groupMemberRepository.lambdaQueryChain()
                .select(GroupMember::getGroupId)
                .eq(GroupMember::getMemberId, memberId)
                .list()
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Group> queryDetailList(List<Long> groupIdList) {
        if (CollectionUtil.isEmpty(groupIdList)) {
            return Collections.emptyList();
        }
        List<Group> groupList = groupRepository.lambdaQueryChain()
                .in(Group::getId, groupIdList).list();
        groupList.forEach(group -> {
            List<Long> routeIdList = groupRouteRepository.lambdaQueryChain()
                    .eq(GroupRoute::getGroupId, group.getId())
                    .list()
                    .stream().map(e -> e.getRouteId()).collect(Collectors.toList());
            List<Long> ruleIdList = groupRouteRuleRepository.lambdaQueryChain()
                    .eq(GroupRouteRule::getGroupId, group.getId())
                    .list()
                    .stream().map(e -> e.getRuleId()).collect(Collectors.toList());
            List<Long> vnatIdList = groupVNATRepository.lambdaQueryChain()
                    .eq(GroupVNAT::getGroupId, group.getId())
                    .list()
                    .stream().map(e -> e.getVnatId()).collect(Collectors.toList());
            group.setRouteIdList(routeIdList);
            group.setRouteRuleIdList(ruleIdList);
            group.setVnatIdList(vnatIdList);
        });
        return groupList;
    }

    @Override
    public Group queryDefaultGroup() {
        Group group = groupRepository.lambdaQueryChain()
                .eq(Group::getDefaultGroup, true)
                .one();
        return group;
    }

    @Override
    public void delAllGroupMember(Long memberId) {
        groupMemberRepository.delete(groupMemberRepository.lambdaQuery()
                .eq(GroupMember::getMemberId, memberId));
    }
}
