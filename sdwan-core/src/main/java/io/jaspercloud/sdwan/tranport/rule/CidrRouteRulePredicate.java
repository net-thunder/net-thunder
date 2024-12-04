package io.jaspercloud.sdwan.tranport.rule;

import io.jaspercloud.sdwan.support.Cidr;

public class CidrRouteRulePredicate implements RouteRulePredicate {

    private Cidr cidr;

    public CidrRouteRulePredicate(Cidr cidr) {
        this.cidr = cidr;
    }

    @Override
    public boolean test(String ip) {
        boolean contains = cidr.contains(ip);
        return contains;
    }
}
