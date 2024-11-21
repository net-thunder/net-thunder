package io.jaspercloud.sdwan.server.entity;

import io.jaspercloud.sdwan.route.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.route.rule.RouteRuleStrategyEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteRule extends BaseEntity {

    private String name;
    private String description;
    private RouteRuleStrategyEnum strategy;
    private RouteRuleDirectionEnum direction;
    private List<String> ruleList;
    private Integer level;
    private List<Long> groupIdList;
    private Boolean enable;
}
