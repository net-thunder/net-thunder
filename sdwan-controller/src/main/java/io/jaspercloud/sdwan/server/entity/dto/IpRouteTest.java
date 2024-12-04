package io.jaspercloud.sdwan.server.entity.dto;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.entity.Route;
import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.tranport.rule.RouteRulePredicateChain;
import io.jaspercloud.sdwan.tranport.rule.RouteRuleStrategyEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class IpRouteTest {

    private String srcIp;
    private String dstIp;
    private List<Message> logList = new ArrayList<>();
    private boolean finish;

    public void request() {
        appendLog(true, String.format("请求 %s -> %s", srcIp, dstIp));
    }

    public void response() {
        String s = srcIp;
        String d = dstIp;
        srcIp = d;
        dstIp = s;
        appendLog(true, String.format("响应 %s -> %s", srcIp, dstIp));
    }

    public void appendLog(boolean status, String log) {
        Message message = new Message();
        message.setSuccess(status);
        message.setMsg(log);
        logList.add(message);
    }

    public void test(Tenant tenant, NodeDetailResponse detail) {
        Cidr vipCidr = Cidr.parseCidr(tenant.getCidr());
        request();
        testOutRoute(vipCidr, detail);
        if (isFinish()) {
            return;
        }
        response();
        testInRoute(vipCidr, detail);
    }

    private void testOutRoute(Cidr vipCidr, NodeDetailResponse detail) {
        if (vipCidr.contains(detail.getVip())) {
            testRule(detail, RouteRuleDirectionEnum.Output, getDstIp());
            return;
        }
        for (VNAT vnat : detail.getVnatList()) {
            String srcCidr = vnat.getSrcCidr();
            String dstCidr = vnat.getDstCidr();
            Cidr cidr = Cidr.parseCidr(srcCidr);
            if (cidr.contains(getDstIp())) {
                appendLog(true, String.format("匹配地址转换: %s -> %s", srcCidr, dstCidr));
                Cidr from = Cidr.parseCidr(srcCidr);
                Cidr to = Cidr.parseCidr(dstCidr);
                String natIp = Cidr.transform(getDstIp(), from, to);
                testRule(detail, RouteRuleDirectionEnum.Output, getDstIp());
                if (isFinish()) {
                    return;
                }
                appendLog(true, String.format("%s -> %s 转换 %s -> %s",
                        getSrcIp(), getDstIp(),
                        getSrcIp(), natIp));
                setDstIp(natIp);
                return;
            }
        }
        for (Route route : detail.getRouteList()) {
            String destination = route.getDestination();
            Cidr cidr = Cidr.parseCidr(destination);
            if (cidr.contains(getDstIp())) {
                appendLog(true, "匹配路由: " + destination);
                testRule(detail, RouteRuleDirectionEnum.Output, getDstIp());
                return;
            }
        }
        appendLog(false, "未匹配路由");
        setFinish(true);
    }

    private void testInRoute(Cidr vipCidr, NodeDetailResponse detail) {
        if (vipCidr.contains(detail.getVip())) {
            testRule(detail, RouteRuleDirectionEnum.Input, getSrcIp());
            return;
        }
        for (VNAT vnat : detail.getVnatList()) {
            String srcCidr = vnat.getSrcCidr();
            String dstCidr = vnat.getDstCidr();
            Cidr cidr = Cidr.parseCidr(dstCidr);
            if (cidr.contains(getSrcIp())) {
                appendLog(true, String.format("匹配地址转换: %s -> %s", dstCidr, srcCidr));
                Cidr from = Cidr.parseCidr(dstCidr);
                Cidr to = Cidr.parseCidr(srcCidr);
                String natIp = Cidr.transform(getSrcIp(), from, to);
                appendLog(true, String.format("%s -> %s 转换 %s -> %s",
                        getSrcIp(), getDstIp(),
                        natIp, getDstIp()));
                setSrcIp(natIp);
                testRule(detail, RouteRuleDirectionEnum.Input, getSrcIp());
                return;
            }
        }
        for (Route route : detail.getRouteList()) {
            String destination = route.getDestination();
            Cidr cidr = Cidr.parseCidr(destination);
            if (cidr.contains(getSrcIp())) {
                appendLog(true, "匹配路由: " + destination);
                testRule(detail, RouteRuleDirectionEnum.Input, getSrcIp());
                return;
            }
        }
        appendLog(false, "未匹配路由");
        setFinish(true);
    }

    private void testRule(NodeDetailResponse detail, RouteRuleDirectionEnum direction, String ip) {
        List<SDWanProtos.RouteRule> routeRuleList = detail.getRouteRuleList().stream()
                .map(e -> SDWanProtos.RouteRule.newBuilder()
                        .setStrategy(e.getStrategy().name())
                        .setDirection(e.getDirection().name())
                        .setLevel(e.getLevel())
                        .addAllRuleList(e.getRuleList())
                        .build())
                .sorted(RouteRulePredicateChain.comparator())
                .collect(Collectors.toList());
        List<RouteRulePredicateChain> predicateList = new ArrayList<>();
        for (SDWanProtos.RouteRule rule : routeRuleList) {
            RouteRulePredicateChain chain = RouteRulePredicateChain.build(rule);
            predicateList.add(chain);
        }
        List<RouteRuleDirectionEnum> allowDirection = Arrays.asList(RouteRuleDirectionEnum.All, direction);
        for (RouteRulePredicateChain predicate : predicateList) {
            if (!allowDirection.contains(predicate.getDirection())) {
                continue;
            }
            if (predicate.test(ip)) {
                if (RouteRuleStrategyEnum.Allow.equals(predicate.getStrategy())) {
                    appendLog(true, "匹配路由规则: 允许" + ip);
                    return;
                } else if (RouteRuleStrategyEnum.Reject.equals(predicate.getStrategy())) {
                    appendLog(false, "匹配路由规则: 拒绝" + ip);
                    setFinish(true);
                    return;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
        appendLog(false, "未匹配路由规则: 拒绝" + ip);
        setFinish(true);
    }

    @Getter
    @Setter
    public static class Message {

        private boolean success;
        private String msg;
    }
}
