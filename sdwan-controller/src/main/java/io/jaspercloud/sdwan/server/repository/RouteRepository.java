package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.entity.RouteNodeItem;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.RouteMapper;
import io.jaspercloud.sdwan.server.repository.po.RoutePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public class RouteRepository extends BaseRepositoryImpl<Route, RoutePO, RouteMapper> {

    @Resource
    private RouteNodeItemRepository routeNodeItemRepository;

    @Override
    public int deleteById(Serializable id) {
        routeNodeItemRepository.delete()
                .eq(RouteNodeItem::getRouteId, id)
                .delete();
        return super.deleteById(id);
    }
}
