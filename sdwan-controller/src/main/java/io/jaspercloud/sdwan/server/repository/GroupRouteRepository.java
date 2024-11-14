package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.GroupRoute;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.GroupRouteMapper;
import io.jaspercloud.sdwan.server.repository.po.GroupRoutePO;
import org.springframework.stereotype.Repository;

@Repository
public class GroupRouteRepository extends BaseRepositoryImpl<GroupRoute, GroupRoutePO, GroupRouteMapper> {

}
