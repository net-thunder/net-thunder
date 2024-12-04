package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.Lifecycle;
import io.jaspercloud.sdwan.tun.TunChannel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class OSRouteTableManager implements Lifecycle {

    private TunChannel tunChannel;
    private OSRouteTable routeTable;
    private AtomicReference<List<SDWanProtos.Route>> cache = new AtomicReference<>(Collections.emptyList());

    public OSRouteTableManager(TunChannel tunChannel, OSRouteTable routeTable) {
        this.tunChannel = tunChannel;
        this.routeTable = routeTable;
    }

    public void update(List<SDWanProtos.Route> routeList) {
        removeRouteList(cache.get());
        routeList = filterRouteList(tunChannel.getTunAddress().getIp(), routeList);
        addRouteList(routeList);
        cache.set(routeList);
    }

    private List<SDWanProtos.Route> filterRouteList(String ip, List<SDWanProtos.Route> routeList) {
        List<SDWanProtos.Route> collect = routeList.stream()
                .filter(e -> !e.getNexthopList().contains(ip))
                .collect(Collectors.toList());
        return collect;
    }

    private void addRouteList(List<SDWanProtos.Route> routeList) {
        for (SDWanProtos.Route route : routeList) {
            routeTable.addRoute(tunChannel, route);
        }
    }

    private void removeRouteList(List<SDWanProtos.Route> routeList) {
        for (SDWanProtos.Route route : routeList) {
            routeTable.deleteRoute(tunChannel, route);
        }
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {
        removeRouteList(cache.get());
    }
}
