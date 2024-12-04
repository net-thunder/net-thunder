package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.tun.TunChannel;
import io.netty.util.internal.PlatformDependent;

public final class OSRouteTableFactory {

    private OSRouteTableFactory() {

    }

    public static OSRouteTableManager create(TunChannel tunChannel) {
        OSRouteTable routeTable;
        if (PlatformDependent.isOsx()) {
            routeTable = new OSOsxRouteTable();
        } else if (PlatformDependent.isWindows()) {
            routeTable = new OSWindowsRouteTable();
        } else {
            routeTable = new OSLinuxRouteTable();
        }
        OSRouteTableManager routeTableManager = new OSRouteTableManager(tunChannel, routeTable);
        return routeTableManager;
    }
}
