package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.NodeRouteRule;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.NodeRouteRuleMapper;
import io.jaspercloud.sdwan.server.repository.po.NodeRouteRulePO;
import org.springframework.stereotype.Repository;

@Repository
public class NodeRouteRuleRepository extends BaseRepository<NodeRouteRule, NodeRouteRulePO, NodeRouteRuleMapper> {

}
