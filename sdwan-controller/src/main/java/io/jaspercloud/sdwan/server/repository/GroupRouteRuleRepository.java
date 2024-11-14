package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.GroupRouteRule;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.GroupRouteRuleMapper;
import io.jaspercloud.sdwan.server.repository.po.GroupRouteRulePO;
import org.springframework.stereotype.Repository;

@Repository
public class GroupRouteRuleRepository extends BaseRepositoryImpl<GroupRouteRule, GroupRouteRulePO, GroupRouteRuleMapper> {

}
