package io.jaspercloud.sdwan.tranport.rule;

import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.IpPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class RouteRulePredicateProcessor {

    private List<RouteRulePredicateChain> predicateList;

    public RouteRulePredicateProcessor() {
        this(Collections.emptyList());
    }

    public RouteRulePredicateProcessor(List<RouteRulePredicateChain> predicateList) {
        this.predicateList = predicateList;
    }

    public boolean test(IpLayerPacket packet, String ip) {
        for (RouteRulePredicateChain predicate : predicateList) {
            if (predicate.test(ip)) {
                if (RouteRuleStrategyEnum.Allow.equals(predicate.getStrategy())) {
                    if (packet.isIcmpProtocol()) {
                        log.info("rule[{}]: Allow srcIP={}, dstIP={}", predicate.getName(), packet.getSrcIP(), packet.getDstIP());
                    }
                    return true;
                } else if (RouteRuleStrategyEnum.Reject.equals(predicate.getStrategy())) {
                    if (packet.isIcmpProtocol()) {
                        log.info("rule[{}]: Reject srcIP={}, dstIP={}", predicate.getName(), packet.getSrcIP(), packet.getDstIP());
                    }
                    return false;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
        return false;
    }
}
