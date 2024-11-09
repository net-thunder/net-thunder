package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.repsitory.RouteRuleMapper;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.RouteRuleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RouteRuleServiceImpl implements RouteRuleService {

    @Resource
    private RouteRuleMapper routeRuleMapper;

    @Resource
    private NodeService nodeService;

    @Override
    public void add(EditRouteRuleRequest request) {
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        routeRule.setId(null);
        routeRule.insert();
    }

    @Override
    public void edit(EditRouteRuleRequest request) {
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        routeRule.updateById();
    }

    @Override
    public void del(EditRouteRuleRequest request) {
        if (nodeService.usedRouteRule(request.getId())) {
            throw new ProcessException("routeRule used");
        }
        routeRuleMapper.deleteById(request.getId());
    }

    @Override
    public PageResponse<RouteRuleResponse> page() {
        Long total = new LambdaQueryChainWrapper<>(routeRuleMapper).count();
        List<RouteRule> list = new LambdaQueryChainWrapper<>(routeRuleMapper)
                .list();
        List<RouteRuleResponse> collect = list.stream().map(e -> {
            RouteRuleResponse routeRuleResponse = BeanUtil.toBean(e, RouteRuleResponse.class);
            return routeRuleResponse;
        }).collect(Collectors.toList());
        PageResponse<RouteRuleResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public List<RouteRule> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<RouteRule> list = new LambdaQueryChainWrapper<>(routeRuleMapper)
                .in(RouteRule::getId, idList)
                .list();
        return list;
    }
}
