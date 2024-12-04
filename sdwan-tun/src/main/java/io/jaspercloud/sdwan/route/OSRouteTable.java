package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.TunChannel;

/**
 * @author jasper
 * @create 2024/7/9
 */
public interface OSRouteTable {

    void addRoute(TunChannel tunChannel, SDWanProtos.Route route);


    void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route);
}
