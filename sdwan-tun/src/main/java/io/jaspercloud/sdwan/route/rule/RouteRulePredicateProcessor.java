package io.jaspercloud.sdwan.route.rule;

import java.util.Collections;
import java.util.List;

public class RouteRulePredicateProcessor {

    private List<RouteRulePredicateChain> predicateList;

    public RouteRulePredicateProcessor() {
        this(Collections.emptyList());
    }

    public RouteRulePredicateProcessor(List<RouteRulePredicateChain> predicateList) {
        this.predicateList = predicateList;
    }

    public boolean test(String ip) {
        for (RouteRulePredicateChain predicate : predicateList) {
            if (predicate.test(ip)) {
                if (RouteRuleStrategyEnum.Allow.equals(predicate.getStrategy())) {
                    return true;
                } else if (RouteRuleStrategyEnum.Reject.equals(predicate.getStrategy())) {
                    return false;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
        return false;
    }
}
