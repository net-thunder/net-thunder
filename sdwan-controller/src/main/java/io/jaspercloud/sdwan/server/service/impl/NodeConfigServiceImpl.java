package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.repository.NodeRepository;
import io.jaspercloud.sdwan.server.repository.GroupRouteRepository;
import io.jaspercloud.sdwan.server.repository.GroupRouteRuleRepository;
import io.jaspercloud.sdwan.server.repository.GroupVNATRepository;
import io.jaspercloud.sdwan.server.repository.po.NodePO;
import io.jaspercloud.sdwan.server.repository.po.GroupRoutePO;
import io.jaspercloud.sdwan.server.repository.po.GroupRouteRulePO;
import io.jaspercloud.sdwan.server.repository.po.GroupVNATPO;
import io.jaspercloud.sdwan.server.service.NodeConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NodeConfigServiceImpl implements NodeConfigService {

    @Resource
    private NodeRepository nodeRepository;

    @Resource
    private GroupRouteRepository groupRouteRepository;

    @Resource
    private GroupRouteRuleRepository groupRouteRuleRepository;

    @Resource
    private GroupVNATRepository groupVNATRepository;

    @Override
    public boolean existsNode(Long nodeId) {
        Long count = nodeRepository.count(nodeRepository.lambdaQuery()
                .eq(NodePO::getId, nodeId));
        return count > 0;
    }

    @Override
    public boolean usedRoute(Long routeId) {
        Long count = groupRouteRepository.count(groupRouteRepository.lambdaQuery()
                .eq(GroupRoutePO::getRouteId, routeId));
        return count > 0;
    }

    @Override
    public boolean usedRouteRule(Long routeRuleId) {
        Long count = groupRouteRuleRepository.count(groupRouteRuleRepository.lambdaQuery()
                .eq(GroupRouteRulePO::getRuleId, routeRuleId));
        return count > 0;
    }

    @Override
    public boolean usedVNAT(Long vnatId) {
        Long count = groupVNATRepository.count(groupVNATRepository.lambdaQuery()
                .eq(GroupVNATPO::getVnatId, vnatId));
        return count > 0;
    }
}
