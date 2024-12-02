package io.jaspercloud.sdwan.server.repository;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.route.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.route.rule.RouteRuleStrategyEnum;
import io.jaspercloud.sdwan.server.controller.request.RouteRuleRequest;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.RouteRuleMapper;
import io.jaspercloud.sdwan.server.repository.po.RouteRulePO;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RouteRuleRepository extends BaseRepositoryImpl<RouteRule, RouteRulePO, RouteRuleMapper> {

    @Override
    protected BeanTransformer.Builder<RouteRule, RouteRulePO> transformerBuilder() {
        return super.transformerBuilder()
                .addFieldMapping(RouteRule::getStrategy, RouteRulePO::getStrategy, e -> {
                    return e.name();
                }, e -> {
                    return RouteRuleStrategyEnum.valueOf(e);
                })
                .addFieldMapping(RouteRule::getDirection, RouteRulePO::getDirection, e -> {
                    return e.name();
                }, e -> {
                    return RouteRuleDirectionEnum.valueOf(e);
                })
                .addFieldMapping(RouteRule::getRuleList, RouteRulePO::getRuleList, e -> {
                    return JSONUtil.toJsonStr(e);
                }, e -> {
                    return JSONUtil.parseArray(e).toList(String.class);
                });
    }

    public List<RouteRule> selectByRequest(RouteRuleRequest request) {
        Map<String, Object> param = new HashMap<>();
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            param.put("groupIdList", request.getGroupIdList());
        }
        if (BooleanUtils.isTrue(request.getEnable())) {
            param.put("enable", request.getEnable());
        }
        List<RouteRule> collect = getBaseMapper().selectByGroup(param)
                .stream()
                .map(e -> getTransformer().output(e))
                .collect(Collectors.toList());
        return collect;
    }
}
