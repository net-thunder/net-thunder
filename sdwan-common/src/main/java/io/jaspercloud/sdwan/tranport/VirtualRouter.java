package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.Cidr;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * @author jasper
 * @create 2024/7/15
 */
public class VirtualRouter {

    private String cidr;
    private List<SDWanProtos.Route> routeList = Collections.emptyList();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Integer, Consumer<List<SDWanProtos.Route>>> listenerMap = new ConcurrentHashMap<>();

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

    public void updateRoutes(List<SDWanProtos.Route> list) {
        lock.writeLock().lock();
        try {
            routeList = list;
        } finally {
            lock.writeLock().unlock();
        }
        listenerMap.forEach((k, v) -> v.accept(routeList));
    }

    public String route(String srcVip, String dstIp) {
        List<SDWanProtos.Route> list;
        lock.readLock().lock();
        try {
            list = routeList;
        } finally {
            lock.readLock().unlock();
        }
        if (Cidr.contains(cidr, dstIp)) {
            return dstIp;
        }
        for (SDWanProtos.Route route : list) {
            if (Cidr.contains(route.getDestination(), dstIp)) {
                ProtocolStringList nexthopList = route.getNexthopList();
                if (nexthopList.isEmpty()) {
                    return null;
                }
                int rand = Math.abs(srcVip.hashCode()) % nexthopList.size();
                String nexthop = nexthopList.get(rand);
                return nexthop;
            }
        }
        return null;
    }
}
