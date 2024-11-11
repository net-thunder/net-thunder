package io.jaspercloud.sdwan.server.service.impl;

import io.jaspercloud.sdwan.server.repository.NodeRouteRepository;
import io.jaspercloud.sdwan.server.repository.NodeRouteRuleRepository;
import io.jaspercloud.sdwan.server.repository.NodeVNATRepository;
import io.jaspercloud.sdwan.server.repository.mapper.NodeMapper;
import io.jaspercloud.sdwan.server.repository.po.NodeRoutePO;
import io.jaspercloud.sdwan.server.repository.po.NodeRouteRulePO;
import io.jaspercloud.sdwan.server.repository.po.NodeVNATPO;
import io.jaspercloud.sdwan.server.service.NodeConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class NodeConfigServiceImpl implements NodeConfigService {

    @Resource
    private NodeMapper nodeMapper;

    @Resource
    private NodeRouteRepository nodeRouteRepository;

    @Resource
    private NodeRouteRuleRepository nodeRouteRuleRepository;

    @Resource
    private NodeVNATRepository nodeVNATRepository;

    @Override
    public boolean usedRoute(Long routeId) {
        Long count = nodeRouteRepository.count(nodeRouteRepository.lambdaQuery()
                .eq(NodeRoutePO::getRouteId, routeId));
        return count > 0;
    }

    @Override
    public boolean usedRouteRule(Long routeRuleId) {
        Long count = nodeRouteRuleRepository.count(nodeRouteRuleRepository.lambdaQuery()
                .eq(NodeRouteRulePO::getRuleId, routeRuleId));
        return count > 0;
    }

    @Override
    public boolean usedVNAT(Long vnatId) {
        Long count = nodeVNATRepository.count(nodeVNATRepository.lambdaQuery()
                .eq(NodeVNATPO::getVnatId, vnatId));
        return count > 0;
    }
}
