package io.jaspercloud.sdwan.tranport.rule;

import org.apache.commons.lang3.StringUtils;

public class Ipv4RouteRulePredicate implements RouteRulePredicate {

    private String ipv4;

    public Ipv4RouteRulePredicate(String ipv4) {
        this.ipv4 = ipv4;
    }

    @Override
    public boolean test(String ip) {
        boolean equals = StringUtils.equals(ip, ipv4);
        return equals;
    }
}
