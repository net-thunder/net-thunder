package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditRouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.request.RouteRuleRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.RouteRuleResponse;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.repository.RouteRuleRepository;
import io.jaspercloud.sdwan.server.repository.po.RouteRulePO;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.RouteRuleService;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleStrategyEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class RouteRuleServiceImpl implements RouteRuleService {

    @Resource
    private RouteRuleRepository routeRuleRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void addDefaultRouteRule(Long groupId) {
        RouteRule rule = new RouteRule();
        rule.setName("default");
        rule.setEnable(true);
        rule.setLevel(100);
        rule.setStrategy(RouteRuleStrategyEnum.Allow);
        rule.setDirection(RouteRuleDirectionEnum.All);
        rule.setRuleList(Arrays.asList("0.0.0.0/0"));
        RouteRulePO rulePO = routeRuleRepository.getTransformer().build(rule);
        rulePO.insert();
        groupConfigService.updateGroupRouteRule(rulePO.getId(), Arrays.asList(groupId));
    }

    @Override
    public void add(EditRouteRuleRequest request) {
        checkUnique(request.getId(), request.getName());
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        RouteRulePO rulePO = routeRuleRepository.getTransformer().build(routeRule);
        rulePO.setId(null);
        rulePO.insert();
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            groupConfigService.updateGroupRouteRule(rulePO.getId(), request.getGroupIdList());
        }
    }

    @Override
    public void edit(EditRouteRuleRequest request) {
        checkUnique(request.getId(), request.getName());
        RouteRule routeRule = BeanUtil.toBean(request, RouteRule.class);
        RouteRulePO rulePO = routeRuleRepository.getTransformer().build(routeRule);
        rulePO.updateById();
        if (null != request.getGroupIdList()) {
            groupConfigService.updateGroupRouteRule(rulePO.getId(), request.getGroupIdList());
        }
    }

    private void checkUnique(Long id, String name) {
        Long count = routeRuleRepository.query()
                .eq(RouteRule::getName, name)
                .func(null != id, w -> {
                    w.ne(RouteRule::getId, id);
                })
                .count();
        if (count > 0) {
            throw new ProcessException("名称已存在");
        }
    }

    @Override
    public void del(EditRouteRuleRequest request) {
        groupConfigService.deleteGroupRouteRule(request.getId());
        routeRuleRepository.deleteById(request.getId());
    }

    @Override
    public List<RouteRuleResponse> list(RouteRuleRequest request) {
        List<RouteRule> list = routeRuleRepository.selectByRequest(request);
        List<RouteRuleResponse> collect = list.stream().map(e -> {
            RouteRuleResponse routeRuleResponse = BeanUtil.toBean(e, RouteRuleResponse.class);
            return routeRuleResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageResponse<RouteRuleResponse> page() {
        Long total = routeRuleRepository.query().count();
        List<RouteRule> list = routeRuleRepository.query()
                .orderByAsc(RouteRule::getLevel)
                .list();
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
