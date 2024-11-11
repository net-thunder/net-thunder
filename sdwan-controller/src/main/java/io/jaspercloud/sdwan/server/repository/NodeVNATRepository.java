package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.NodeVNAT;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.NodeVNATMapper;
import io.jaspercloud.sdwan.server.repository.po.NodeVNATPO;
import org.springframework.stereotype.Repository;

@Repository
public class NodeVNATRepository extends BaseRepository<NodeVNAT, NodeVNATPO, NodeVNATMapper> {

}
