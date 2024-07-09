package io.jaspercloud.sdwan;

import io.jaspercloud.sdwan.tranport.P2pClient;
import io.jaspercloud.sdwan.util.SocketAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class P2pTransportManager implements InitializingBean, Runnable {

    private P2pClient p2pClient;
    private long heartTime;

    private Map<String, Set<InetSocketAddress>> p2pAddressMap = new ConcurrentHashMap<>();

    public void clearP2pAddress() {
        p2pAddressMap.clear();
    }

    public Set<InetSocketAddress> getP2pAddressSet(String vip) {
        Set<InetSocketAddress> addressSet = p2pAddressMap.get(vip);
        if (null == addressSet) {
            addressSet = Collections.emptySet();
        }
        return addressSet;
    }

    public void addP2pAddress(String vip, InetSocketAddress address) {
        Set<InetSocketAddress> set = p2pAddressMap.computeIfAbsent(vip, key -> new ConcurrentSkipListSet<>());
        set.add(address);
    }

    public P2pTransportManager(P2pClient p2pClient, long heartTime) {
        this.p2pClient = p2pClient;
        this.heartTime = heartTime;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this, 0, heartTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        Iterator<String> iterator = p2pAddressMap.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Set<InetSocketAddress> addressSet = p2pAddressMap.get(key);
            testAddressSet(addressSet);
            if (addressSet.isEmpty()) {
                p2pAddressMap.remove(key);
            }
        }
    }

    private void testAddressSet(Set<InetSocketAddress> addressSet) {
        Iterator<InetSocketAddress> iterator = addressSet.iterator();
        while (iterator.hasNext()) {
            InetSocketAddress address = iterator.next();
            try {
                log.debug("p2p heart: {}", SocketAddressUtil.toAddress(address));
                p2pClient.parseNATAddress(address, 3000).get();
            } catch (Exception e) {
                iterator.remove();
            }
        }
    }
}
