package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.osx.OsxTunDevice;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;

/**
 * @author jasper
 * @create 2024/7/9
 */
public class OsxRouteManager implements RouteManager {

    @Override
    public void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        String cmd = String.format("route -n add -net %s -interface %s", route.getDestination(), ethName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        String cmd = String.format("route -n delete -net %s -interface %s", route.getDestination(), ethName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0, 2);
    }
}
