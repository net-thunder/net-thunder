package io.jaspercloud.sdwan.route.rule;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RouteRulePredicateProcessor implements RouteRulePredicate {

    private List<RouteRulePredicate> rejectList;
    private List<RouteRulePredicate> allowList;

    public RouteRulePredicateProcessor() {
        this(Collections.emptyList());
    }

    public RouteRulePredicateProcessor(List<RouteRulePredicate> predicateList) {
        this.rejectList = predicateList.stream()
                .filter(e -> RouteRuleStrategyEnum.Reject.equals(e.strategy()))
                .collect(Collectors.toList());
        this.allowList = predicateList.stream()
                .filter(e -> RouteRuleStrategyEnum.Allow.equals(e.strategy()))
                .collect(Collectors.toList());
    }

    @Override
    public RouteRuleStrategyEnum strategy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RouteRuleDirectionEnum direction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean test(String ip) {
        for (RouteRulePredicate predicate : rejectList) {
            if (false == predicate.test(ip)) {
                return false;
            }
        }
        for (RouteRulePredicate predicate : allowList) {
            if (true == predicate.test(ip)) {
                return true;
            }
        }
        return false;
    }
}
