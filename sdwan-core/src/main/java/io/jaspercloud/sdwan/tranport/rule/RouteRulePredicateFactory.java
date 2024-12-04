package io.jaspercloud.sdwan.tranport.rule;

import cn.hutool.core.lang.PatternPool;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.util.IPUtil;

public final class RouteRulePredicateFactory {

    private RouteRulePredicateFactory() {

    }

    public static RouteRulePredicate parse(String rule) {
        if (PatternPool.IPV4.matcher(rule).find()) {
            return new Ipv4RouteRulePredicate(rule);
        } else if (rule.contains("-")) {
            Ipv4RangeRouteRulePredicate rangePredicate = parseIpRange(rule);
            return rangePredicate;
        } else {
            try {
                Cidr cidr = Cidr.parseCidr(rule);
                return new CidrRouteRulePredicate(cidr);
            } catch (Exception e) {
                throw new ProcessException("规则格式错误: " + rule);
            }
        }
    }

    private static Ipv4RangeRouteRulePredicate parseIpRange(String range) {
        String[] split = range.split("-");
        if (split.length != 2) {
            throw new ProcessException("规则格式错误: " + range);
        }
        if (!PatternPool.IPV4.matcher(split[0]).find()) {
            throw new ProcessException("规则格式错误: " + range);
        }
        if (!PatternPool.IPV4.matcher(split[1]).find()) {
            throw new ProcessException("规则格式错误: " + range);
        }
        long start = IPUtil.ip2long(split[0]);
        long end = IPUtil.ip2long(split[1]);
        return new Ipv4RangeRouteRulePredicate(start, end);
    }
}
