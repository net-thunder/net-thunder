package io.jaspercloud.sdwan.server.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.RouteMapper;
import io.jaspercloud.sdwan.server.repository.po.RouteNodeItemPO;
import io.jaspercloud.sdwan.server.repository.po.RoutePO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RouteRepository extends BaseRepository<Route, RoutePO, RouteMapper> {

    @Resource
    private RouteNodeItemRepository routeNodeItemRepository;

    @Override
    public List<Route> list(Wrapper<RoutePO> queryWrapper) {
        List<Route> list = super.list(queryWrapper);
        list.forEach(route -> {
            List<Long> collect = routeNodeItemRepository.list(routeNodeItemRepository.lambdaQuery()
                            .select(RouteNodeItemPO::getNodeId)
                            .eq(RouteNodeItemPO::getRouteId, route.getId()))
                    .stream().map(e -> e.getNodeId()).collect(Collectors.toList());
            route.setNodeIdList(collect);
        });
        return list;
    }

    @Override
    public int deleteById(Serializable id) {
        routeNodeItemRepository.delete(routeNodeItemRepository.lambdaQuery()
                .eq(RouteNodeItemPO::getRouteId, id));
        return super.deleteById(id);
    }
}
