package io.jaspercloud.sdwan.route;

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;

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
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<Integer, Consumer<List<SDWanProtos.Route>>> listenerMap = new ConcurrentHashMap<>();
    private List<SDWanProtos.Route> routeList = Collections.emptyList();
    private Map<String, String> transformMap = new ConcurrentHashMap<>();

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
            transformMap.clear();
            routeList = list;
        } finally {
            lock.writeLock().unlock();
        }
        listenerMap.forEach((k, v) -> v.accept(routeList));
    }

    public SDWanProtos.IpPacket routeIn(Ipv4Packet packet) {
        String srcIp = transformMap.get(packet.getSrcIP());
        if (null != srcIp) {
            packet.setSrcIP(srcIp);
        }
        byte[] bytes = ByteBufUtil.toBytesRelease(packet.encode());
        SDWanProtos.IpPacket ipPacket = SDWanProtos.IpPacket.newBuilder()
                .setSrcIP(packet.getSrcIP())
                .setDstIP(packet.getDstIP())
                .setPayload(ByteString.copyFrom(bytes))
                .build();
        return ipPacket;
    }

    public String routeOut(Ipv4Packet packet) {
        String dstIP = packet.getDstIP();
        if (Cidr.contains(cidr, dstIP)) {
            return dstIP;
        }
        List<SDWanProtos.Route> list;
        lock.readLock().lock();
        try {
            list = routeList;
        } finally {
            lock.readLock().unlock();
        }
        for (SDWanProtos.Route route : list) {
            if (Cidr.contains(route.getDestination(), dstIP)) {
                ProtocolStringList nexthopList = route.getNexthopList();
                if (nexthopList.isEmpty()) {
                    return null;
                }
                if (null != route.getTransform()) {
                    Cidr from = Cidr.parseCidr(route.getDestination());
                    Cidr to = Cidr.parseCidr(route.getTransform());
                    String transformIp = Cidr.transform(dstIP, from, to);
                    transformMap.put(transformIp, dstIP);
                    packet.setDstIP(transformIp);
                }
                int rand = Math.abs(packet.getSrcIP().hashCode()) % nexthopList.size();
                String nexthop = nexthopList.get(rand);
                return nexthop;
            }
        }
        return null;
    }
}
