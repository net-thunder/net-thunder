package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.stun.StunMessage;
import io.jaspercloud.sdwan.support.AsyncTask;
import io.jaspercloud.sdwan.tranport.DataTransport;
import io.jaspercloud.sdwan.tranport.TransportLifecycle;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class P2pTransportManager implements TransportLifecycle, Runnable {

    private SdWanNodeConfig config;
    private AtomicBoolean status = new AtomicBoolean();
    private ScheduledExecutorService scheduledExecutorService;
    private Map<String, TransportWrapper> transportMap = new ConcurrentHashMap<>();
    private Map<String, String> heartMap = new ConcurrentHashMap<>();

    public void clear() {
        transportMap.clear();
    }

    public DataTransport get(String ip) {
        TransportWrapper ref = transportMap.get(ip);
        if (null == ref) {
            return null;
        }
        DataTransport transport = ref.getTransport();
        return transport;
    }

    public TransportWrapper getOrCreate(String vip, Consumer<TransportWrapper> consumer) {
        return transportMap.computeIfAbsent(vip, key -> {
            log.info("getOrCreateDataTransport ip: {}", vip);
            TransportWrapper ref = new TransportWrapper();
            consumer.accept(ref);
            return ref;
        });
    }

    public void addTransport(String vip, DataTransport transport) {
        log.info("addTransport vip: {}", vip);
        TransportWrapper wrapper = new TransportWrapper();
        wrapper.setTransport(transport);
        transportMap.put(vip, wrapper);
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
        if (!status.get()) {
            return;
        }
        try {
            for (String vip : transportMap.keySet()) {
                TransportWrapper reference = transportMap.get(vip);
                if (null == reference) {
                    continue;
                }
                DataTransport transport = reference.getTransport();
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

    @Override
    public boolean isRunning() {
        return status.get();
    }

    @Override
    public void start() {
        status.set(true);
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(this, 0, config.getP2pCheckTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        status.set(false);
        scheduledExecutorService.shutdown();
    }
}
