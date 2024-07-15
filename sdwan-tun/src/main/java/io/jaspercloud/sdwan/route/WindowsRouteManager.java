package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.VirtualRouter;
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
public class WindowsRouteManager extends AbstractRouteManager {

    public WindowsRouteManager(TunChannel tunChannel, VirtualRouter virtualRouter) {
        super(tunChannel, virtualRouter);
    }

    @Override
    protected void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(tunAddress.getIp());
        String cmd = String.format("route add %s %s if %s", route.getDestination(), tunAddress.getIp(), interfaceInfo.getIndex());
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    protected void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception {
        TunAddress tunAddress = (TunAddress) tunChannel.localAddress();
        NetworkInterfaceInfo interfaceInfo = NetworkInterfaceUtil.findNetworkInterfaceInfo(tunAddress.getIp());
        // TODO: 2023/11/24 路由删除失败: 找不到元素。
//        String cmd = String.format("route delete %s %s", route.getDestination(), tunAddress.getVip());
        String cmd = String.format("route delete %s", route.getDestination());
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }
}
