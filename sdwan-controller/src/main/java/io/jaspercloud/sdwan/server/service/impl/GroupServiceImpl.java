package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditGroupMemberRequest;
import io.jaspercloud.sdwan.server.controller.request.EditGroupRequest;
import io.jaspercloud.sdwan.server.controller.response.GroupResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.entity.GroupMember;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.repsitory.GroupMapper;
import io.jaspercloud.sdwan.server.repsitory.GroupMemberMapper;
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
    private GroupMapper groupMapper;

    @Resource
    private GroupMemberMapper groupMemberMapper;

    @Resource
    private NodeService nodeService;

    @Override
    public void add(EditGroupRequest request) {
        Group group = BeanUtil.toBean(request, Group.class);
        group.setId(null);
        group.insert();
    }

    @Override
    public void edit(EditGroupRequest request) {
        Group group = BeanUtil.toBean(request, Group.class);
        group.updateById();
    }

    @Override
    public void del(EditGroupRequest request) {
        Long count = new LambdaQueryChainWrapper<>(groupMemberMapper)
                .eq(GroupMember::getMemberId, request.getId())
                .count();
        if (count > 0) {
            throw new ProcessException("group used");
        }
        groupMapper.deleteById(request.getId());
    }

    @Override
    public PageResponse<GroupResponse> page() {
        Long total = new LambdaQueryChainWrapper<>(groupMapper).count();
        List<Group> list = new LambdaQueryChainWrapper<>(groupMapper)
                .list();
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
            GroupMember member = new GroupMember();
            member.setGroupId(request.getGroupId());
            member.setMemberId(id);
            member.insert();
        }
    }

    @Override
    public void delMember(EditGroupMemberRequest request) {
        List<GroupMember> memberList = new LambdaQueryChainWrapper<>(groupMemberMapper)
                .eq(GroupMember::getGroupId, request.getGroupId())
                .in(GroupMember::getMemberId, request.getMemberIdList())
                .list();
        for (GroupMember member : memberList) {
            member.deleteById();
        }
    }

    @Override
    public List<Node> memberList(Long groupId) {
        List<Long> idList = new LambdaQueryChainWrapper<>(groupMemberMapper)
                .eq(GroupMember::getGroupId, groupId)
                .list().stream().map(e -> e.getMemberId()).collect(Collectors.toList());
        List<Node> nodeList = nodeService.queryByIdList(idList);
        return nodeList;
    }

    @Override
    public List<Group> queryByMemberId(Long memberId) {
        List<Long> idList = new LambdaQueryChainWrapper<>(groupMemberMapper)
                .select(GroupMember::getGroupId)
                .eq(GroupMember::getMemberId, memberId)
                .list()
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Group> groupList = groupMapper.selectByIds(idList);
        return groupList;
    }

}
