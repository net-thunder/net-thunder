package io.jaspercloud.sdwan.route.rule;

import java.util.function.Predicate;

public interface RouteRulePredicate extends Predicate<String> {

    @Override
    boolean test(String ip);
}
