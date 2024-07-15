package io.jaspercloud.sdwan.tranport;

import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.Cidr;
import org.apache.commons.lang3.RandomUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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
    private Set<Consumer<List<SDWanProtos.Route>>> listenerSet = new ConcurrentSkipListSet<>();

    public void addListener(Consumer<List<SDWanProtos.Route>> listener) {
        listenerSet.add(listener);
    }

    public void removeListener(Consumer<List<SDWanProtos.Route>> listener) {
        listenerSet.remove(listener);
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
        listenerSet.forEach(e -> e.accept(routeList));
    }

    public String route(String ip) {
        List<SDWanProtos.Route> list;
        lock.readLock().lock();
        try {
            list = routeList;
        } finally {
            lock.readLock().unlock();
        }
        if (Cidr.contains(cidr, ip)) {
            return ip;
        }
        for (SDWanProtos.Route route : list) {
            if (Cidr.contains(route.getDestination(), ip)) {
                ProtocolStringList nexthopList = route.getNexthopList();
                if (nexthopList.isEmpty()) {
                    return null;
                }
                int rand = RandomUtils.nextInt(0, nexthopList.size());
                String nexthop = nexthopList.get(rand);
                return nexthop;
            }
        }
        return null;
    }
}
