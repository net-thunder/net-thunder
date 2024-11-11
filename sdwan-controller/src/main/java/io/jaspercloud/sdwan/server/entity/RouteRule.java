package io.jaspercloud.sdwan.server.entity;

import cn.hutool.json.JSONUtil;
import io.jaspercloud.sdwan.server.repository.po.RouteRulePO;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteRule extends BaseEntity {

    private String name;
    private String description;
    private DirectionEnum direction;
    private List<String> ruleList;
    private Boolean enable;

    public static final BeanTransformer<RouteRule, RouteRulePO> Transformer = BeanTransformer.builder(RouteRule.class, RouteRulePO.class)
            .addFieldMapping(RouteRule::getDirection, RouteRulePO::getDirection, e -> {
                return e.name();
            }, e -> {
                return DirectionEnum.valueOf(e);
            })
            .addFieldMapping(RouteRule::getRuleList, RouteRulePO::getRuleList, e -> {
                return JSONUtil.toJsonStr(e);
            }, e -> {
                return JSONUtil.parseArray(e).toList(String.class);
            })
            .build();
}
