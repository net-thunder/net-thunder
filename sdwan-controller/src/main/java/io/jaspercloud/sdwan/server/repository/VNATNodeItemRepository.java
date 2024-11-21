package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.VNATNodeItem;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.VNATNodeItemMapper;
import io.jaspercloud.sdwan.server.repository.po.VNATNodeItemPO;
import org.springframework.stereotype.Repository;

@Repository
public class VNATNodeItemRepository extends BaseRepositoryImpl<VNATNodeItem, VNATNodeItemPO, VNATNodeItemMapper> {

}
