package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.NodeRoute;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.NodeRouteMapper;
import io.jaspercloud.sdwan.server.repository.po.NodeRoutePO;
import org.springframework.stereotype.Repository;

@Repository
public class NodeRouteRepository extends BaseRepository<NodeRoute, NodeRoutePO, NodeRouteMapper> {

}
