package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.entity.GroupRoute;
import io.jaspercloud.sdwan.server.entity.GroupRouteRule;
import io.jaspercloud.sdwan.server.entity.GroupVNAT;
import io.jaspercloud.sdwan.server.repository.GroupRepository;
import io.jaspercloud.sdwan.server.repository.GroupRouteRepository;
import io.jaspercloud.sdwan.server.repository.GroupRouteRuleRepository;
import io.jaspercloud.sdwan.server.repository.GroupVNATRepository;
import io.jaspercloud.sdwan.server.repository.po.GroupRoutePO;
import io.jaspercloud.sdwan.server.repository.po.GroupRouteRulePO;
import io.jaspercloud.sdwan.server.repository.po.GroupVNATPO;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupConfigServiceImpl implements GroupConfigService {

    @Resource
    private GroupRepository groupRepository;

    @Resource
    private GroupRouteRepository groupRouteRepository;

    @Resource
    private GroupRouteRuleRepository groupRouteRuleRepository;

    @Resource
    private GroupVNATRepository groupVNATRepository;

    @Override
    public boolean usedRoute(Long routeId) {
        Long count = groupRouteRepository.query()
                .eq(GroupRoute::getRouteId, routeId)
                .count();
        return count > 0;
    }

    @Override
    public boolean usedRouteRule(Long routeRuleId) {
        Long count = groupRouteRuleRepository.query()
                .eq(GroupRouteRule::getRuleId, routeRuleId)
                .count();
        return count > 0;
    }

    @Override
    public boolean usedVNAT(Long vnatId) {
        Long count = groupVNATRepository.query()
                .eq(GroupVNAT::getVnatId, vnatId)
                .count();
        return count > 0;
    }

    @Override
    public void updateGroupRoute(Long routeId, List<Long> groupIdList) {
        groupRouteRepository.delete()
                .eq(GroupRoute::getRouteId, routeId)
                .delete();
        if (CollectionUtil.isEmpty(groupIdList)) {
            return;
        }
        Set<Long> ids = groupRepository.selectBatchIds(groupIdList)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        for (Long groupId : groupIdList) {
            if (!ids.contains(groupId)) {
                throw new ProcessException("not found group");
            }
            GroupRoutePO groupRoutePO = new GroupRoutePO();
            groupRoutePO.setRouteId(routeId);
            groupRoutePO.setGroupId(groupId);
            groupRoutePO.insert();
        }
    }

    @Override
    public void updateGroupRouteRule(Long routeRuleId, List<Long> groupIdList) {
        groupRouteRuleRepository.delete()
                .eq(GroupRouteRule::getRuleId, routeRuleId)
                .delete();
        if (CollectionUtil.isEmpty(groupIdList)) {
            return;
        }
        Set<Long> ids = groupRepository.selectBatchIds(groupIdList)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        for (Long groupId : groupIdList) {
            if (!ids.contains(groupId)) {
                throw new ProcessException("not found group");
            }
            GroupRouteRulePO groupRouteRulePO = new GroupRouteRulePO();
            groupRouteRulePO.setRuleId(routeRuleId);
            groupRouteRulePO.setGroupId(groupId);
            groupRouteRulePO.insert();
        }
    }

    @Override
    public void updateGroupVNAT(Long vnatId, List<Long> groupIdList) {
        groupVNATRepository.delete()
                .eq(GroupVNAT::getVnatId, vnatId)
                .delete();
        if (CollectionUtil.isEmpty(groupIdList)) {
            return;
        }
        Set<Long> ids = groupRepository.selectBatchIds(groupIdList)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        for (Long groupId : groupIdList) {
            if (!ids.contains(groupId)) {
                throw new ProcessException("not found group");
            }
            GroupVNATPO groupVNATPO = new GroupVNATPO();
            groupVNATPO.setVnatId(vnatId);
            groupVNATPO.setGroupId(groupId);
            groupVNATPO.insert();
        }
    }

    @Override
    public List<Long> queryGroupRouteList(Long routeId) {
        List<Long> collect = groupRouteRepository.query()
                .select(GroupRoute::getGroupId)
                .eq(GroupRoute::getRouteId, routeId)
                .list()
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Long> queryGroupRouteRuleList(Long routeRuleId) {
        List<Long> collect = groupRouteRuleRepository.query()
                .select(GroupRouteRule::getGroupId)
                .eq(GroupRouteRule::getRuleId, routeRuleId)
                .list()
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Long> queryGroupVNATList(Long vnatId) {
        List<Long> collect = groupVNATRepository.query()
                .select(GroupVNAT::getGroupId)
                .eq(GroupVNAT::getVnatId, vnatId)
                .list()
                .stream().map(e -> e.getGroupId()).collect(Collectors.toList());
        return collect;
    }
}
