package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.tun.osx.OsxTunDevice;
import lombok.extern.slf4j.Slf4j;

/**
 * query: netstat -nr
 *
 * @author jasper
 * @create 2024/7/9
 */
@Slf4j
public class OSOsxRouteTable implements OSRouteTable {

    @Override
    public void addRoute(TunChannel tunChannel, SDWanProtos.Route route) {
        try {
            OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
            String ethName = tunDevice.getEthName();
            String cmd = String.format("route -n add -net %s -interface %s", route.getDestination(), ethName);
            log.info("addRoute: {}", cmd);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) {
        try {
            OsxTunDevice tunDevice = (OsxTunDevice) tunChannel.getTunDevice();
            String ethName = tunDevice.getEthName();
            String cmd = String.format("route -n delete -net %s -interface %s", route.getDestination(), ethName);
            log.info("deleteRoute: {}", cmd);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0, 2);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
