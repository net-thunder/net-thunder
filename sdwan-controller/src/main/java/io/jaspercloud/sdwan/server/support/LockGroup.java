package io.jaspercloud.sdwan.server.support;

import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockGroup {

    private Map<Long, Lock> lockMap = new ConcurrentHashMap<>();

    public Lock getLock(Long tenantId) {
        Lock lock = lockMap.computeIfAbsent(tenantId, k -> new Lock(new ReentrantLock()));
        lock.lock();
        return lock;
    }

    public static class Lock implements Closeable {

        private ReentrantLock rLock;

        public Lock(ReentrantLock rLock) {
            this.rLock = rLock;
        }

        public void lock() {
            rLock.lock();
        }

        @Override
        public void close() {
            rLock.unlock();
        }
    }
}
