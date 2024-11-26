package io.jaspercloud.sdwan.node.rule;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.route.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.route.rule.RouteRulePredicate;
import io.jaspercloud.sdwan.route.rule.RouteRuleStrategyEnum;
import io.jaspercloud.sdwan.support.Cidr;

import java.util.List;
import java.util.stream.Collectors;

public class CidrRouteRulePredicate implements RouteRulePredicate {

    private SDWanProtos.RouteRule routeRule;
    private RouteRuleStrategyEnum strategy;
    private RouteRuleDirectionEnum direction;
    private List<Cidr> cidrList;

    public CidrRouteRulePredicate(SDWanProtos.RouteRule routeRule) {
        this.routeRule = routeRule;
        this.strategy = RouteRuleStrategyEnum.valueOf(routeRule.getStrategy());
        this.direction = RouteRuleDirectionEnum.valueOf(routeRule.getDirection());
        this.cidrList = routeRule.getRuleListList().stream()
                .map(e -> Cidr.parseCidr(e)).collect(Collectors.toList());
    }

    @Override
    public RouteRuleStrategyEnum strategy() {
        return strategy;
    }

    @Override
    public RouteRuleDirectionEnum direction() {
        return direction;
    }

    @Override
    public boolean test(String ip) {
        String strategy = routeRule.getStrategy();
        long count = cidrList.stream().filter(e -> e.contains(ip)).count();
        if (RouteRuleStrategyEnum.Allow.name().equals(strategy)) {
            return count > 0;
        } else if (RouteRuleStrategyEnum.Reject.name().equals(strategy)) {
            return count <= 0;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
