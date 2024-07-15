package io.jaspercloud.sdwan.route;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.tranport.VirtualRouter;
import io.jaspercloud.sdwan.tun.TunChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author jasper
 * @create 2024/7/9
 */
@Slf4j
public abstract class AbstractRouteManager implements RouteManager, Consumer<List<SDWanProtos.Route>> {

    private TunChannel tunChannel;
    private VirtualRouter virtualRouter;
    private AtomicBoolean status = new AtomicBoolean(false);
    private AtomicReference<List<SDWanProtos.Route>> cache = new AtomicReference<>(Collections.emptyList());
    private Lock lock = new ReentrantLock();

    public AbstractRouteManager(TunChannel tunChannel, VirtualRouter virtualRouter) {
        this.tunChannel = tunChannel;
        this.virtualRouter = virtualRouter;
    }

    @Override
    public void start() throws Exception {
        lock.lock();
        try {
            status.set(true);
            virtualRouter.addListener(this);
            for (SDWanProtos.Route route : virtualRouter.getRouteList()) {
                addRoute(tunChannel, route);
            }
            cache.set(virtualRouter.getRouteList());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            status.set(false);
            virtualRouter.removeListener(this);
            for (SDWanProtos.Route route : cache.get()) {
                try {
                    deleteRoute(tunChannel, route);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void accept(List<SDWanProtos.Route> routes) {
        lock.lock();
        try {
            if (!status.get()) {
                return;
            }
            for (SDWanProtos.Route route : cache.get()) {
                try {
                    deleteRoute(tunChannel, route);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            for (SDWanProtos.Route route : routes) {
                try {
                    addRoute(tunChannel, route);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            cache.set(routes);
        } finally {
            lock.unlock();
        }
    }

    protected abstract void addRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception;

    protected abstract void deleteRoute(TunChannel tunChannel, SDWanProtos.Route route) throws Exception;
}
