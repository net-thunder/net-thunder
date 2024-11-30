package io.jaspercloud.sdwan.route.rule;

import java.util.Collections;
import java.util.List;

public class RouteRulePredicateProcessor implements RouteRulePredicate {

    private List<RouteRuleChain> rejectList;
    private List<RouteRuleChain> predicateList;

    public RouteRulePredicateProcessor() {
        this(Collections.emptyList());
    }

    public RouteRulePredicateProcessor(List<RouteRuleChain> predicateList) {
        this.predicateList = predicateList;
    }

    @Override
    public boolean test(String ip) {
        for (RouteRuleChain predicate : predicateList) {
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
