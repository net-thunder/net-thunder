package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
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
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            groupConfigService.updateGroupRoute(rulePO.getId(), request.getGroupIdList());
        }
    }

    @Override
    public void edit(EditRouteRuleRequest request) {
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        RouteRulePO rulePO = RouteRule.Transformer.build(routeRule);
        rulePO.updateById();
        if (null != request.getGroupIdList()) {
            groupConfigService.updateGroupRouteRule(rulePO.getId(), request.getGroupIdList());
        }
    }

    @Override
    public void del(EditRouteRuleRequest request) {
        groupConfigService.deleteGroupRouteRule(request.getId());
        routeRuleRepository.deleteById(request.getId());
    }

    @Override
    public List<RouteRuleResponse> list() {
        List<RouteRule> list = routeRuleRepository.query().list();
        List<RouteRuleResponse> collect = list.stream().map(e -> {
            RouteRuleResponse routeRuleResponse = BeanUtil.toBean(e, RouteRuleResponse.class);
            return routeRuleResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageResponse<RouteRuleResponse> page() {
        Long total = routeRuleRepository.query().count();
        List<RouteRule> list = routeRuleRepository.query().list();
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
    public RouteRule queryDetailById(Long id) {
        RouteRule routeRule = routeRuleRepository.selectById(id);
        if (null == routeRule) {
            return null;
        }
        List<Long> list = groupConfigService.queryGroupRouteRuleList(id);
        routeRule.setGroupIdList(list);
        return routeRule;
    }

    @Override
    public List<RouteRule> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<RouteRule> list = routeRuleRepository.query()
                .in(RouteRule::getId, idList)
                .list();
        return list;
    }
}
