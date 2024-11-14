package io.jaspercloud.sdwan.server.repository;

import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.server.enums.DirectionEnum;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.RouteRuleMapper;
import io.jaspercloud.sdwan.server.repository.po.RouteRulePO;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import org.springframework.stereotype.Repository;

@Repository
public class RouteRuleRepository extends BaseRepositoryImpl<RouteRule, RouteRulePO, RouteRuleMapper> {

    @Override
    protected BeanTransformer.Builder<RouteRule, RouteRulePO> transformerBuilder() {
        return super.transformerBuilder()
                .addFieldMapping(RouteRule::getDirection, RouteRulePO::getDirection, e -> {
                    return e.name();
                }, e -> {
                    return DirectionEnum.valueOf(e);
                })
                .addFieldMapping(RouteRule::getRuleList, RouteRulePO::getRuleList, e -> {
                    return JSONUtil.toJsonStr(e);
                }, e -> {
                    return JSONUtil.parseArray(e).toList(String.class);
                });
    }
}
