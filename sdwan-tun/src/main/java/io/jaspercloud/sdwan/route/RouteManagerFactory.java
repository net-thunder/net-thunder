package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.tun.TunChannel;
import io.netty.util.internal.PlatformDependent;

public final class RouteManagerFactory {

    private RouteManagerFactory() {

    }

    public static RouteManager create(TunChannel tunChannel, VirtualRouter virtualRouter) {
        RouteManager routeManager;
        if (PlatformDependent.isOsx()) {
            routeManager = new OsxRouteManager(tunChannel, virtualRouter);
        } else if (PlatformDependent.isWindows()) {
            routeManager = new WindowsRouteManager(tunChannel, virtualRouter);
        } else {
            routeManager = new LinuxRouteManager(tunChannel, virtualRouter);
        }
        return routeManager;
    }
}
