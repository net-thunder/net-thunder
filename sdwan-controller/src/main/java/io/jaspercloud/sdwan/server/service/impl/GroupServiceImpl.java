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
@Transactional(rollbackFor = Exception.class)
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
        checkUnique(request.getId(), request.getName());
        GroupPO group = BeanUtil.toBean(request, GroupPO.class);
        group.setId(null);
        group.setDefaultGroup(false);
        group.insert();
    }

    @Override
    public void edit(EditGroupRequest request) {
        checkUnique(request.getId(), request.getName());
        GroupPO group = BeanUtil.toBean(request, GroupPO.class);
        group.updateById();
    }

    private void checkUnique(Long id, String name) {
        Long count = groupRepository.query()
                .eq(Group::getName, name)
                .func(null != id, w -> {
                    w.ne(Group::getId, id);
                })
                .count();
        if (count > 0) {
            throw new ProcessException("名称已存在");
        }
    }

    @Override
    public void del(EditGroupRequest request) {
        Long memberCount = groupMemberRepository.query()
                .eq(GroupMember::getMemberId, request.getId())
                .count();
        if (memberCount > 0) {
            throw new ProcessException("被组内成员使用");
        }
        Long routeCount = groupRouteRepository.query()
                .eq(GroupRoute::getGroupId, request.getId())
                .count();
        if (routeCount > 0) {
            throw new ProcessException("被路由使用");
        }
        Long routeRuleCount = groupRouteRuleRepository.query()
                .eq(GroupRouteRule::getGroupId, request.getId())
                .count();
        if (routeRuleCount > 0) {
            throw new ProcessException("被路由规则使用");
        }
        Long vnatCount = groupVNATRepository.query()
                .eq(GroupVNAT::getGroupId, request.getId())
                .count();
        if (vnatCount > 0) {
            throw new ProcessException("被地址转换使用");
        }
        groupRepository.deleteById(request.getId());
    }

    @Override
    public List<GroupResponse> list() {
        List<Group> list = groupRepository.query().list();
        List<GroupResponse> collect = list.stream().map(e -> {
            GroupResponse groupResponse = BeanUtil.toBean(e, GroupResponse.class);
            return groupResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageResponse<GroupResponse> page() {
        Long total = groupRepository.query().count();
        List<Group> list = groupRepository.query().list();
        List<GroupResponse> collect = list.stream().map(e -> {
            GroupResponse groupResponse = BeanUtil.toBean(e, GroupResponse.class);
            return groupResponse;
        }).collect(Collectors.toList());
        PageResponse<GroupResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public void addMember(Long groupId, Long memberId) {
        Long count = groupMemberRepository.query()
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
        groupMemberRepository.delete()
                .eq(GroupMember::getGroupId, groupId)
                .delete();
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
        List<Long> idList = groupMemberRepository.query()
                .eq(GroupMember::getGroupId, groupId)
                .list()
                .stream().map(e -> e.getMemberId()).collect(Collectors.toList());
        return idList;
    }

    @Override
    public List<Group> queryByMemberId(Long memberId) {
        List<Long> idList = groupMemberRepository.query()
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
        List<Long> collect = groupMemberRepository.query()
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
        List<Group> groupList = groupRepository.query()
                .in(Group::getId, groupIdList).list();
        groupList.forEach(group -> {
            List<Long> routeIdList = groupRouteRepository.query()
                    .eq(GroupRoute::getGroupId, group.getId())
                    .list()
                    .stream().map(e -> e.getRouteId()).collect(Collectors.toList());
            List<Long> ruleIdList = groupRouteRuleRepository.query()
                    .eq(GroupRouteRule::getGroupId, group.getId())
                    .list()
                    .stream().map(e -> e.getRuleId()).collect(Collectors.toList());
            List<Long> vnatIdList = groupVNATRepository.query()
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
    public List<Group> queryByIdList(List<Long> groupIdList) {
        if (CollectionUtil.isEmpty(groupIdList)) {
            return Collections.emptyList();
        }
        List<Group> groupList = groupRepository.query()
                .in(Group::getId, groupIdList).list();
        return groupList;
    }

    @Override
    public Group queryDefaultGroup() {
        Group group = groupRepository.query()
                .eq(Group::getDefaultGroup, true)
                .one();
        return group;
    }

    @Override
    public void delAllGroupMember(Long memberId) {
        groupMemberRepository.delete()
                .eq(GroupMember::getMemberId, memberId)
                .delete();
    }
}
