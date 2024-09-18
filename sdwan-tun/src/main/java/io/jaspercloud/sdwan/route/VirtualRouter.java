package io.jaspercloud.sdwan.route;

import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author jasper
 * @create 2024/7/15
 */
public class VirtualRouter {

    private String cidr;
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Integer, Consumer<List<SDWanProtos.Route>>> listenerMap = new ConcurrentHashMap<>();
    private List<SDWanProtos.Route> routeList = Collections.emptyList();
    private Map<String, SDWanProtos.VNAT> vnatMap = new ConcurrentHashMap();

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

    public void updateVNATs(List<SDWanProtos.VNAT> vnatList) {
        Map<String, SDWanProtos.VNAT> map = new ConcurrentHashMap();
        vnatList.forEach(e -> {
            map.put(e.getSrc(), e);
            map.put(e.getDst(), e);
        });
        lock.writeLock().lock();
        try {
            vnatMap = map;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public IpLayerPacket routeIn(IpLayerPacket packet) {
        SDWanProtos.VNAT vnat = findVNAT(packet, IpLayerPacket::getDstIP);
        if (null == vnat) {
            return packet;
        }
        String natIp = natInIp(vnat, packet.getDstIP());
        packet.setDstIP(natIp);
        return packet;
    }


    public String routeOut(IpLayerPacket packet) {
        String dstIP = packet.getDstIP();
        if (Cidr.contains(cidr, dstIP)) {
            return dstIP;
        }
        SDWanProtos.VNAT vnat = findVNAT(packet, IpLayerPacket::getSrcIP);
        if (null != vnat) {
            String natIp = natOutIp(vnat, packet.getSrcIP());
            packet.setSrcIP(natIp);
            String outIp = packet.getDstIP();
            return outIp;
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

    private SDWanProtos.VNAT findVNAT(IpLayerPacket packet, Function<IpLayerPacket, String> in) {
        Map<String, SDWanProtos.VNAT> map;
        lock.readLock().lock();
        try {
            map = vnatMap;
        } finally {
            lock.readLock().unlock();
        }
        for (Map.Entry<String, SDWanProtos.VNAT> entry : map.entrySet()) {
            String ip = in.apply(packet);
            String key = entry.getKey();
            if (StringUtils.equals(ip, key)) {
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
