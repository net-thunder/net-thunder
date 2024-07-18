package io.jaspercloud.sdwan.support;

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

    public AtomicReference<DataTransport> getOrCreate(String ip, Consumer<String> consumer) {
        return transportMap.computeIfAbsent(ip, key -> {
            AtomicReference<DataTransport> ref = new AtomicReference<>();
            consumer.accept(key);
            return ref;
        });
    }

    public void addTransport(String ip, DataTransport transport) {
        transportMap.put(ip, new AtomicReference<>(transport));
    }

    public void deleteTransport(String ip) {
        transportMap.remove(ip);
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
                    log.debug("timeout remove transport: {}", key);
                } else {
                    log.error(e.getMessage(), e);
                    log.debug("remove transport: {}", key);
                }
                iterator.remove();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                log.debug("remove transport: {}", key);
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
