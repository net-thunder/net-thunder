package io.jaspercloud.sdwan.route;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.route.rule.RouteRuleChain;
import io.jaspercloud.sdwan.route.rule.RouteRuleDirectionEnum;
import io.jaspercloud.sdwan.route.rule.RouteRulePredicateProcessor;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tranport.TransportLifecycle;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class VirtualRouter implements TransportLifecycle {

    private volatile String cidr;
    private volatile boolean showRouteRuleLog = false;
    private volatile List<SDWanProtos.Route> routeList = Collections.emptyList();
    private volatile RouteRulePredicateProcessor routeInRuleProcessor = new RouteRulePredicateProcessor();
    private volatile RouteRulePredicateProcessor routeOutRuleProcessor = new RouteRulePredicateProcessor();
    private volatile Map<String, SDWanProtos.VNAT> vnatMap = Collections.emptyMap();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Integer, Consumer<List<SDWanProtos.Route>>> listenerMap = new ConcurrentHashMap<>();
    private Cache<String, String> vnatMappingCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    private AtomicBoolean status = new AtomicBoolean(false);

    private Map<String, SDWanProtos.VNAT> getVnatMap() {
        lock.readLock().lock();
        try {
            return vnatMap;
        } finally {
            lock.readLock().unlock();
        }
    }

    public VirtualRouter() {
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

    public void setShowRouteRuleLog(boolean showRouteRuleLog) {
        this.showRouteRuleLog = showRouteRuleLog;
    }

    public List<SDWanProtos.Route> getRouteList() {
        lock.readLock().lock();
        try {
            return routeList;
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

    public void updateRouteRules(List<RouteRuleChain> list) {
        lock.writeLock().lock();
        try {
            List<RouteRuleChain> routeInRuleList = list.stream()
                    .filter(e -> Arrays.asList(RouteRuleDirectionEnum.All, RouteRuleDirectionEnum.Input).contains(e.getDirection()))
                    .collect(Collectors.toList());
            List<RouteRuleChain> routeOutRuleList = list.stream()
                    .filter(e -> Arrays.asList(RouteRuleDirectionEnum.All, RouteRuleDirectionEnum.Output).contains(e.getDirection()))
                    .collect(Collectors.toList());
            routeInRuleProcessor = new RouteRulePredicateProcessor(routeInRuleList);
            routeOutRuleProcessor = new RouteRulePredicateProcessor(routeOutRuleList);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateVNATs(List<SDWanProtos.VNAT> vnatList) {
        Map<String, SDWanProtos.VNAT> map = new ConcurrentHashMap();
        vnatList.forEach(e -> {
            log.info("addVNAT: src={}, dst={}, vipList={}", e.getSrc(), e.getDst(), e.getVipListList());
            map.put(e.getSrc(), e);
        });
        lock.writeLock().lock();
        try {
            vnatMap = map;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public IpLayerPacket routeIn(IpLayerPacket packet) {
        if (!routeInRuleProcessor.test(packet.getDstIP())) {
            if (showRouteRuleLog) {
                log.info("reject routeIn: {}", packet);
            }
            return null;
        }
        SDWanProtos.VNAT vnat = findVNAT(getVnatMap(), packet, IpLayerPacket::getDstIP);
        if (null == vnat) {
            return packet;
        }
        String dstIP = packet.getDstIP();
        String natIp = natIp(vnat, dstIP);
        packet.setDstIP(natIp);
        String identifier = packet.getIdentifier(true);
        vnatMappingCache.put(identifier, dstIP);
        return packet;
    }

    public String routeOut(IpLayerPacket packet) {
        if (!routeOutRuleProcessor.test(packet.getDstIP())) {
            if (showRouteRuleLog) {
                log.info("reject routeOut: {}", packet);
            }
            return null;
        }
        String originalIp = vnatMappingCache.getIfPresent(packet.getIdentifier());
        if (null != originalIp) {
            packet.setSrcIP(originalIp);
        }
        String dstIP = packet.getDstIP();
        if (Cidr.contains(cidr, dstIP)) {
            return dstIP;
        }
        dstIP = findRoute(packet);
        return dstIP;
    }

    private String natIp(SDWanProtos.VNAT vnat, String ip) {
        Cidr from = Cidr.parseCidr(vnat.getSrc());
        Cidr to = Cidr.parseCidr(vnat.getDst());
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

    @Override
    public void start() throws Exception {
        status.set(true);
    }

    @Override
    public void stop() throws Exception {
        vnatMappingCache.cleanUp();
        status.set(false);
    }

    @Override
    public boolean isRunning() {
        return status.get();
    }
}
