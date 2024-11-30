package io.jaspercloud.sdwan.route.rule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RouteRulePredicateProcessor implements RouteRulePredicate {

    private List<RouteRuleChain> rejectList;
    private List<RouteRuleChain> allowList;

    public RouteRulePredicateProcessor() {
        this(Collections.emptyList());
    }

    public RouteRulePredicateProcessor(List<RouteRuleChain> predicateList) {
        this.rejectList = predicateList.stream()
                .filter(e -> RouteRuleStrategyEnum.Reject.equals(e.getStrategy()))
                .collect(Collectors.toList());
        this.allowList = predicateList.stream()
                .filter(e -> RouteRuleStrategyEnum.Allow.equals(e.getStrategy()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean test(String ip) {
        for (RouteRuleChain predicate : rejectList) {
            if (false == predicate.test(ip)) {
                return false;
            }
        }
        for (RouteRuleChain predicate : allowList) {
            if (true == predicate.test(ip)) {
                return true;
            }
        }
        return false;
    }
}
