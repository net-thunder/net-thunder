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
    private boolean pass = true;

    public void appendLog(boolean status, String log) {
        Message message = new Message();
        message.setSuccess(status);
        message.setMsg(log);
        logList.add(message);
    }

    public void test(Tenant tenant, NodeDetailResponse detail) {
        Cidr vipCidr = Cidr.parseCidr(tenant.getCidr());
        showHost(srcIp);
        inputLog("ping", srcIp, dstIp);
        testOutRoute(vipCidr, detail, srcIp, dstIp);
        if (isFinish()) {
            checkPass();
            return;
        }
        outputLog("ping", srcIp, dstIp);
        process(vipCidr, detail);
        if (isFinish()) {
            checkPass();
            return;
        }
        showHost(srcIp);
        inputLog("pong", dstIp, srcIp);
        testInRoute(vipCidr, detail, dstIp, srcIp);
        checkPass();
    }

    private void checkPass() {
        if (isPass()) {
            appendLog(true, "通过");
        } else {
            appendLog(false, "失败");
        }
    }

    private void showHost(String ip) {
        appendLog(true, String.format("主机 %s", ip));
    }

    private void process(Cidr vipCidr, NodeDetailResponse detail) {
        showHost(dstIp);
        inputLog("ping", srcIp, dstIp);
        testInRoute(vipCidr, detail, srcIp, dstIp);
        if (isFinish()) {
            return;
        }
        testOutRoute(vipCidr, detail, srcIp, dstIp);
        outputLog("pong", dstIp, srcIp);
    }

    private void inputLog(String protocol, String srcIp, String dstIp) {
        appendLog(true, String.format("接收 %s %s -> %s", protocol, srcIp, dstIp));
    }

    private void outputLog(String protocol, String srcIp, String dstIp) {
        appendLog(true, String.format("发送 %s %s -> %s", protocol, srcIp, dstIp));
    }

    private void testOutRoute(Cidr vipCidr, NodeDetailResponse detail, String srcIp, String dstIp) {
        if (vipCidr.contains(dstIp)) {
            testRule(detail, RouteRuleDirectionEnum.Output, dstIp);
            return;
        }
        for (VNAT vnat : detail.getVnatList()) {
            String srcCidr = vnat.getSrcCidr();
            String dstCidr = vnat.getDstCidr();
            Cidr cidr = Cidr.parseCidr(srcCidr);
            if (cidr.contains(dstIp)) {
                appendLog(true, String.format("匹配地址转换: %s -> %s", srcCidr, dstCidr));
                Cidr from = Cidr.parseCidr(srcCidr);
                Cidr to = Cidr.parseCidr(dstCidr);
                String natIp = Cidr.transform(dstIp, from, to);
                testRule(detail, RouteRuleDirectionEnum.Output, dstIp);
                if (isFinish()) {
                    return;
                }
                appendLog(true, String.format("%s -> %s 转换 %s -> %s", srcIp, dstIp, srcIp, natIp));
                setDstIp(natIp);
                return;
            }
        }
        for (Route route : detail.getRouteList()) {
            String destination = route.getDestination();
            Cidr cidr = Cidr.parseCidr(destination);
            if (cidr.contains(dstIp)) {
                appendLog(true, String.format("匹配路由[%s]: %s", route.getName(), destination));
                testRule(detail, RouteRuleDirectionEnum.Output, dstIp);
                return;
            }
        }
        appendLog(false, "未匹配路由");
        setFinish(true);
        setPass(false);
    }

    private void testInRoute(Cidr vipCidr, NodeDetailResponse detail, String srcIp, String dstIp) {
        if (vipCidr.contains(detail.getVip())) {
            testRule(detail, RouteRuleDirectionEnum.Input, srcIp);
            return;
        }
        for (VNAT vnat : detail.getVnatList()) {
            String srcCidr = vnat.getSrcCidr();
            String dstCidr = vnat.getDstCidr();
            Cidr cidr = Cidr.parseCidr(dstCidr);
            if (cidr.contains(srcIp)) {
                appendLog(true, String.format("匹配地址转换: %s -> %s", dstCidr, srcCidr));
                Cidr from = Cidr.parseCidr(dstCidr);
                Cidr to = Cidr.parseCidr(srcCidr);
                String natIp = Cidr.transform(srcIp, from, to);
                appendLog(true, String.format("%s -> %s 转换 %s -> %s", srcIp, dstIp, natIp, dstIp));
                setSrcIp(natIp);
                testRule(detail, RouteRuleDirectionEnum.Input, srcIp);
                return;
            }
        }
        for (Route route : detail.getRouteList()) {
            String destination = route.getDestination();
            Cidr cidr = Cidr.parseCidr(destination);
            if (cidr.contains(srcIp)) {
                appendLog(true, String.format("匹配路由[%s]: %s", route.getName(), destination));
                testRule(detail, RouteRuleDirectionEnum.Input, srcIp);
                return;
            }
        }
        appendLog(false, "未匹配路由");
        setFinish(true);
        setPass(false);
    }

    private void testRule(NodeDetailResponse detail, RouteRuleDirectionEnum direction, String ip) {
        List<SDWanProtos.RouteRule> routeRuleList = detail.getRouteRuleList().stream().map(e -> SDWanProtos.RouteRule.newBuilder().setName(e.getName()).setStrategy(e.getStrategy().name()).setDirection(e.getDirection().name()).setLevel(e.getLevel()).addAllRuleList(e.getRuleList()).build()).sorted(RouteRulePredicateChain.comparator()).collect(Collectors.toList());
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
                String directionText;
                if (RouteRuleDirectionEnum.Input.equals(direction)) {
                    directionText = "入口";
                } else if (RouteRuleDirectionEnum.Output.equals(direction)) {
                    directionText = "出口";
                } else {
                    throw new UnsupportedOperationException();
                }
                if (RouteRuleStrategyEnum.Allow.equals(predicate.getStrategy())) {
                    appendLog(true, String.format("匹配规则[%s]: 允许%s %s", predicate.getName(), directionText, ip));
                    return;
                } else if (RouteRuleStrategyEnum.Reject.equals(predicate.getStrategy())) {
                    appendLog(false, String.format("匹配规则[%s]: 拒绝%s %s", predicate.getName(), directionText, ip));
                    setFinish(true);
                    setPass(false);
                    return;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }
        appendLog(false, "未匹配规则: 拒绝" + ip);
        setFinish(true);
        setPass(false);
    }

    @Getter
    @Setter
    public static class Message {

        private boolean success;
        private String msg;

        @Override
        public String toString() {
            return "Message{" + "success=" + success + ", msg='" + msg + '\'' + '}';
        }
    }
}
