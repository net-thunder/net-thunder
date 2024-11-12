package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.GroupRouteRule;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.GroupRouteRuleMapper;
import io.jaspercloud.sdwan.server.repository.po.GroupRouteRulePO;
import org.springframework.stereotype.Repository;

@Repository
public class GroupRouteRuleRepository extends BaseRepository<GroupRouteRule, GroupRouteRulePO, GroupRouteRuleMapper> {

}
