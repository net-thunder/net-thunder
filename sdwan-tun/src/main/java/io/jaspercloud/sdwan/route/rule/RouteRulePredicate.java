package io.jaspercloud.sdwan.route.rule;

import java.util.function.Predicate;

public interface RouteRulePredicate extends Predicate<String> {

    RouteRuleDirectionEnum direction();
}
