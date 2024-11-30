package io.jaspercloud.sdwan.route.rule;

import io.jaspercloud.sdwan.util.IPUtil;

public class Ipv4RangeRouteRulePredicate implements RouteRulePredicate {

    private long start;
    private long end;

    public Ipv4RangeRouteRulePredicate(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean test(String ip) {
        long address = IPUtil.ip2long(ip);
        if (address >= start && address <= end) {
            return true;
        } else {
            return false;
        }
    }
}
