package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.tranport.DataTransport;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class P2pTransportManager implements Runnable {

    private long heartTime;

    private ScheduledExecutorService scheduledExecutorService;
    private Map<String, AtomicReference<DataTransport>> transportMap = new ConcurrentHashMap<>();

    public void clear() {
        transportMap.clear();
    }

    public DataTransport get(String ip) {
        AtomicReference<DataTransport> ref = transportMap.get(ip);
        if (null == ref) {
            return null;
        }
        DataTransport transport = ref.get();
        return transport;
    }

    public AtomicReference<DataTransport> getOrCreate(String vip, Consumer<String> consumer) {
        return transportMap.computeIfAbsent(vip, key -> {
            log.info("getOrCreateDataTransport ip: {}", vip);
            AtomicReference<DataTransport> ref = new AtomicReference<>();
            consumer.accept(key);
            return ref;
        });
    }

    public void addTransport(String vip, DataTransport transport) {
        log.info("addTransport vip: {}", vip);
        transportMap.put(vip, new AtomicReference<>(transport));
    }

    public void deleteTransport(String vip) {
        log.info("deleteTransport vip: {}", vip);
        transportMap.remove(vip);
    }

    public P2pTransportManager(long heartTime) {
        this.heartTime = heartTime;
    }

    @Override
    public void run() {
        Iterator<String> iterator = transportMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            DataTransport transport = transportMap.get(key).get();
            if (null == transport) {
                continue;
            }
            try {
                transport.ping(3000);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    log.info("timeout remove vip: {}", key);
                } else {
                    log.error(e.getMessage(), e);
                    log.info("remove vip: {}", key);
                }
                iterator.remove();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                log.info("remove vip: {}", key);
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
