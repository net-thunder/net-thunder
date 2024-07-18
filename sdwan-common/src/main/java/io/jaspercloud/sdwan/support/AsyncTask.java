package io.jaspercloud.sdwan.support;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncTask {

    public static final Timer TIMEOUT = new HashedWheelTimer(
            new DefaultThreadFactory("async-task-timeout", true),
            20, TimeUnit.MILLISECONDS);
    private static Map<String, CompletableFuture> futureMap = new ConcurrentHashMap<>();

    public static void waitTime(long timeout, TimerTask timerTask) {
        TIMEOUT.newTimeout(timerTask, timeout, TimeUnit.MILLISECONDS);
    }

    public static <T> CompletableFuture<T> create(long timeout) {
        FutureTask futureTask = new FutureTask(UUID.randomUUID().toString());
        futureTask.timeoutTask = TIMEOUT.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                futureTask.completeExceptionally(new TimeoutException());
            }
        }, timeout, TimeUnit.MILLISECONDS);
        return futureTask;
    }

    public static <T> CompletableFuture<T> waitTask(String id, long timeout) {
        FutureTask futureTask = new FutureTask(id);
        futureMap.put(id, futureTask);
        futureTask.timeoutTask = TIMEOUT.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                completeExceptionallyTask(id, new TimeoutException());
            }
        }, timeout, TimeUnit.MILLISECONDS);
        return futureTask;
    }

    public static void completeTask(String id, Object result) {
        CompletableFuture future = futureMap.remove(id);
        if (null != future && !future.isDone()) {
            future.complete(result);
        }
    }

    public static void completeExceptionallyTask(String id, Throwable throwable) {
        CompletableFuture future = futureMap.remove(id);
        if (null != future && !future.isDone()) {
            future.completeExceptionally(throwable);
        }
    }

    private static class FutureTask extends CompletableFuture {

        private String id;
        private Timeout timeoutTask;

        public FutureTask(String id) {
            this.id = id;
        }

        @Override
        public boolean complete(Object value) {
            timeoutTask.cancel();
            return super.complete(value);
        }
    }
}
