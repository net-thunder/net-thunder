package io.jaspercloud.sdwan.route.rule;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;

import java.util.ArrayList;
import java.util.List;

public class RouteRuleChain {

    private RouteRuleStrategyEnum strategy;
    private RouteRuleDirectionEnum direction;
    private List<RouteRulePredicate> predicateList;

    public RouteRuleStrategyEnum getStrategy() {
        return strategy;
    }

    public RouteRuleDirectionEnum getDirection() {
        return direction;
    }

    public RouteRuleChain(RouteRuleStrategyEnum strategy, RouteRuleDirectionEnum direction, List<RouteRulePredicate> predicateList) {
        this.strategy = strategy;
        this.direction = direction;
        this.predicateList = predicateList;
    }

    public boolean test(String ip) {
        long count = predicateList.stream().filter(e -> e.test(ip)).count();
        return count > 0;
    }

    public static RouteRuleChain build(SDWanProtos.RouteRule routeRule) {
        List<RouteRulePredicate> list = new ArrayList<>();
        for (String rule : routeRule.getRuleListList()) {
            RouteRulePredicate rulePredicate = RouteRulePredicateFactory.parse(rule);
            list.add(rulePredicate);
        }
        RouteRuleStrategyEnum strategy = RouteRuleStrategyEnum.valueOf(routeRule.getStrategy());
        RouteRuleDirectionEnum direction = RouteRuleDirectionEnum.valueOf(routeRule.getDirection());
        RouteRuleChain chain = new RouteRuleChain(strategy, direction, list);
        return chain;
    }
}
