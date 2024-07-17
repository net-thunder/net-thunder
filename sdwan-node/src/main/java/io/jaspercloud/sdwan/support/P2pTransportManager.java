package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.tranport.P2pClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class P2pTransportManager implements Runnable {

    private P2pClient p2pClient;
    private long heartTime;

    private ScheduledExecutorService scheduledExecutorService;
    private Map<String, AtomicReference<IceClient.Transport>> transportMap = new ConcurrentHashMap<>();

    public void clear() {
        transportMap.clear();
    }

    public IceClient.Transport get(String ip) {
        AtomicReference<IceClient.Transport> ref = transportMap.get(ip);
        if (null == ref) {
            return null;
        }
        IceClient.Transport transport = ref.get();
        return transport;
    }

    public AtomicReference<IceClient.Transport> getOrCreate(String ip, Consumer<String> consumer) {
        return transportMap.computeIfAbsent(ip, key -> {
            AtomicReference<IceClient.Transport> ref = new AtomicReference<>();
            consumer.accept(key);
            return ref;
        });
    }

    public void addTransport(String ip, IceClient.Transport transport) {
        transportMap.put(ip, new AtomicReference<>(transport));
    }

    public void deleteTransport(String ip) {
        transportMap.remove(ip);
    }

    public P2pTransportManager(P2pClient p2pClient, long heartTime) {
        this.p2pClient = p2pClient;
        this.heartTime = heartTime;
    }

    @Override
    public void run() {
        Iterator<String> iterator = transportMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            IceClient.Transport transport = transportMap.get(key).get();
            if (null == transport) {
                continue;
            }
            try {
                transport.ping(3000);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                iterator.remove();
            }
        }
    }

    public void start() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this, 0, heartTime, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
