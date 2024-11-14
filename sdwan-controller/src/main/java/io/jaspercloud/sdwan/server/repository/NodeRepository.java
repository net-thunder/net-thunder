package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.NodeMapper;
import io.jaspercloud.sdwan.server.repository.po.NodePO;
import org.springframework.stereotype.Repository;

@Repository
public class NodeRepository extends BaseRepositoryImpl<Node, NodePO, NodeMapper> {

}
