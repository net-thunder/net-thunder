package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.RouteNodeItem;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.RouteNodeItemMapper;
import io.jaspercloud.sdwan.server.repository.po.RouteNodeItemPO;
import org.springframework.stereotype.Repository;

@Repository
public class RouteNodeItemRepository extends BaseRepositoryImpl<RouteNodeItem, RouteNodeItemPO, RouteNodeItemMapper> {

}
