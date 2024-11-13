package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.repository.RouteRuleRepository;
import io.jaspercloud.sdwan.server.repository.po.RouteRulePO;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
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
    private RouteRuleRepository routeRuleRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void add(EditRouteRuleRequest request) {
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        RouteRulePO rulePO = RouteRule.Transformer.build(routeRule);
        rulePO.setId(null);
        rulePO.insert();
    }

    @Override
    public void edit(EditRouteRuleRequest request) {
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        RouteRulePO rulePO = RouteRule.Transformer.build(routeRule);
        rulePO.updateById();
    }

    @Override
    public void del(EditRouteRuleRequest request) {
        if (groupConfigService.usedRouteRule(request.getId())) {
            throw new ProcessException("routeRule used");
        }
        routeRuleRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<RouteRuleResponse> page() {
        Long total = routeRuleRepository.count();
        List<RouteRule> list = routeRuleRepository.list();
        List<RouteRuleResponse> collect = list.stream().map(e -> {
            RouteRuleResponse routeRuleResponse = BeanUtil.toBean(e, RouteRuleResponse.class);
            return routeRuleResponse;
        }).collect(Collectors.toList());
        PageResponse<RouteRuleResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public RouteRule queryById(Long id) {
        RouteRule routeRule = routeRuleRepository.selectById(id);
        return routeRule;
    }

    @Override
    public List<RouteRule> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<RouteRule> list = routeRuleRepository.list(routeRuleRepository.lambdaQuery()
                .in(RouteRule::getId, idList));
        return list;
    }
}
