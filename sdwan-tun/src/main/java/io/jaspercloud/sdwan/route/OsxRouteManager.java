package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.VirtualRouter;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.osx.OsxTunDevice;

/**
 * query: netstat -nr
 *
 * @author jasper
 * @create 2024/7/9
 */
public class OsxRouteManager extends AbstractRouteManager {

    public OsxRouteManager(TunChannel tunChannel, VirtualRouter virtualRouter) {
        super(tunChannel, virtualRouter);
    }

    @Override
    protected void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        String cmd = String.format("route -n add -net %s -interface %s", route.getDestination(), ethName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    protected void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
        String ethName = tunDevice.getEthName();
        String cmd = String.format("route -n delete -net %s -interface %s", route.getDestination(), ethName);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0, 2);
    }
}
