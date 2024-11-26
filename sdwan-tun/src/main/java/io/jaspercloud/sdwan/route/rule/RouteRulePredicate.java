package io.jaspercloud.sdwan.route.rule;

import java.util.function.Predicate;

public interface RouteRulePredicate extends Predicate<String> {

    RouteRuleStrategyEnum strategy();

    RouteRuleDirectionEnum direction();

    @Override
    boolean test(String ip);
}
