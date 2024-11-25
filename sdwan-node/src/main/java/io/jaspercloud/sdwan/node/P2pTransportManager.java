package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.stun.StunMessage;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.tranport.DataTransport;
import lombok.extern.slf4j.Slf4j;

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

    private SdWanNodeConfig config;
    private ScheduledExecutorService scheduledExecutorService;
    private Map<String, AtomicReference<DataTransport>> transportMap = new ConcurrentHashMap<>();
    private Map<String, String> heartMap = new ConcurrentHashMap<>();

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

    public P2pTransportManager(SdWanNodeConfig config) {
        this.config = config;
    }

    @Override
    public void run() {
        try {
            for (String vip : transportMap.keySet()) {
                DataTransport transport = transportMap.get(vip).get();
                if (null == transport) {
                    continue;
                }
                String address = transport.addressUri().toString();
                String id = heartMap.computeIfAbsent(address, key -> {
                    String tranId = StunMessage.genTranId();
                    AsyncTask.waitTask(tranId, config.getP2pCheckTimeout())
                            .whenComplete((msg, ex) -> {
                                try {
                                    if (null == ex) {
                                        return;
                                    }
                                    log.info("ping transport timeout: vip={}, address={}", vip, address);
                                    transportMap.remove(vip);
                                } finally {
                                    heartMap.remove(key);
                                }
                            });
                    return tranId;
                });
                transport.sendPingOneWay(id);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    public void start() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this, 0, config.getP2pCheckTime(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduledExecutorService.shutdown();
    }
}
