package io.jaspercloud.sdwan.tranport.rule;

import java.util.function.Predicate;

public interface RouteRulePredicate extends Predicate<String> {

    @Override
    boolean test(String ip);
}
