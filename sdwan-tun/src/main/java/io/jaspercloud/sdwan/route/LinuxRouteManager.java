package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunChannel;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jasper
 * @create 2024/7/9
 */
@Slf4j
public class LinuxRouteManager extends AbstractRouteManager {

    public LinuxRouteManager(TunChannel tunChannel, VirtualRouter virtualRouter) {
        super(tunChannel, virtualRouter);
    }

    @Override
    protected void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findIp(tunAddress.getIp());
        String cmd = String.format("ip route add %s via %s dev %s", route.getDestination(), tunAddress.getIp(), interfaceInfo.getEthName());
        log.info("addRoute: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    protected void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findIp(tunAddress.getIp());
        String cmd = String.format("ip route delete %s via %s", route.getDestination(), tunAddress.getIp());
        log.info("deleteRoute: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0, 2);
    }
}
