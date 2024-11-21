package io.jaspercloud.sdwan.route;

import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.route.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.route.rule.RouteRulePredicate;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jasper
 * @create 2024/7/15
 */
@Slf4j
public class VirtualRouter {

    private String cidr;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Integer, Consumer<List<SDWanProtos.Route>>> listenerMap = new ConcurrentHashMap<>();
    private List<SDWanProtos.Route> routeList = Collections.emptyList();
    private List<RouteRulePredicate> routeInRuleList = Collections.emptyList();
    private List<RouteRulePredicate> routeOutRuleList = Collections.emptyList();
    private Map<String, SDWanProtos.VNAT> vnatInMap = Collections.emptyMap();
    private Map<String, SDWanProtos.VNAT> vnatOutMap = Collections.emptyMap();

    private Map<String, SDWanProtos.VNAT> getVnatInMap() {
        lock.readLock().lock();
        try {
            return vnatInMap;
        } finally {
            lock.readLock().unlock();
        }
    }

    private Map<String, SDWanProtos.VNAT> getVnatOutMap() {
        lock.readLock().lock();
        try {
            return vnatOutMap;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addListener(Consumer<List<SDWanProtos.Route>> listener) {
        listenerMap.put(listener.hashCode(), listener);
    }

    public void removeListener(Consumer<List<SDWanProtos.Route>> listener) {
        listenerMap.remove(listener.hashCode());
    }

    public void updateCidr(String cidr) {
        this.cidr = cidr;
    }

    public List<SDWanProtos.Route> getRouteList() {
        lock.readLock().lock();
        try {
            return routeList;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RouteRulePredicate> getRouteInRuleList() {
        lock.readLock().lock();
        try {
            return routeInRuleList;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RouteRulePredicate> getRouteOutRuleList() {
        lock.readLock().lock();
        try {
            return routeOutRuleList;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateRoutes(List<SDWanProtos.Route> list) {
        lock.writeLock().lock();
        try {
            routeList = list;
        } finally {
            lock.writeLock().unlock();
        }
        listenerMap.forEach((k, v) -> v.accept(routeList));
    }

    public void updateRouteRules(List<RouteRulePredicate> list) {
        lock.writeLock().lock();
        try {
            routeInRuleList = list.stream()
                    .filter(e -> Arrays.asList(RouteRuleDirectionEnum.All, RouteRuleDirectionEnum.Input).contains(e.direction()))
                    .collect(Collectors.toList());
            routeOutRuleList = list.stream()
                    .filter(e -> Arrays.asList(RouteRuleDirectionEnum.All, RouteRuleDirectionEnum.Output).contains(e.direction()))
                    .collect(Collectors.toList());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateVNATs(List<SDWanProtos.VNAT> vnatList) {
        Map<String, SDWanProtos.VNAT> inMap = new ConcurrentHashMap();
        Map<String, SDWanProtos.VNAT> outMap = new ConcurrentHashMap();
        vnatList.forEach(e -> {
            log.info("addVNAT: src={}, dst={}, vip={}", e.getSrc(), e.getDst(), e.getVip());
            inMap.put(e.getSrc(), e);
            outMap.put(e.getDst(), e);
        });
        lock.writeLock().lock();
        try {
            vnatInMap = inMap;
            vnatOutMap = outMap;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public IpLayerPacket routeIn(IpLayerPacket packet) {
        for (RouteRulePredicate predicate : routeInRuleList) {
            if (!predicate.test(packet.getDstIP())) {
                return null;
            }
        }
        SDWanProtos.VNAT vnat = findVNAT(getVnatInMap(), packet, IpLayerPacket::getDstIP);
        if (null == vnat) {
            return packet;
        }
        String natIp = natInIp(vnat, packet.getDstIP());
        packet.setDstIP(natIp);
        return packet;
    }

    public String routeOut(IpLayerPacket packet) {
        for (RouteRulePredicate predicate : routeOutRuleList) {
            if (!predicate.test(packet.getDstIP())) {
                return null;
            }
        }
        String dstIP = packet.getDstIP();
        SDWanProtos.VNAT vnat = findVNAT(getVnatOutMap(), packet, IpLayerPacket::getSrcIP);
        if (null != vnat) {
            String natIp = natOutIp(vnat, packet.getSrcIP());
            packet.setSrcIP(natIp);
        }
        if (Cidr.contains(cidr, dstIP)) {
            return dstIP;
        }
        dstIP = findRoute(packet);
        return dstIP;
    }

    private String natInIp(SDWanProtos.VNAT vnat, String ip) {
        Cidr from = Cidr.parseCidr(vnat.getSrc());
        Cidr to = Cidr.parseCidr(vnat.getDst());
        String natIp = Cidr.transform(ip, from, to);
        return natIp;
    }

    private String natOutIp(SDWanProtos.VNAT vnat, String ip) {
        Cidr from = Cidr.parseCidr(vnat.getDst());
        Cidr to = Cidr.parseCidr(vnat.getSrc());
        String natIp = Cidr.transform(ip, from, to);
        return natIp;
    }

    private SDWanProtos.VNAT findVNAT(Map<String, SDWanProtos.VNAT> map, IpLayerPacket packet, Function<IpLayerPacket, String> in) {
        for (Map.Entry<String, SDWanProtos.VNAT> entry : map.entrySet()) {
            String ip = in.apply(packet);
            String cidr = entry.getKey();
            if (Cidr.contains(cidr, ip)) {
                SDWanProtos.VNAT vnat = entry.getValue();
                return vnat;
            }
        }
        return null;
    }

    private String findRoute(IpLayerPacket packet) {
        List<SDWanProtos.Route> list;
        lock.readLock().lock();
        try {
            list = routeList;
        } finally {
            lock.readLock().unlock();
        }
        String dstIP = packet.getDstIP();
        for (SDWanProtos.Route route : list) {
            if (Cidr.contains(route.getDestination(), dstIP)) {
                ProtocolStringList nexthopList = route.getNexthopList();
                if (nexthopList.isEmpty()) {
                    return null;
                }
                int rand = Math.abs(packet.getSrcIP().hashCode()) % nexthopList.size();
                String nexthop = nexthopList.get(rand);
                return nexthop;
            }
        }
        return null;
    }
}
