package io.jaspercloud.sdwan.support;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CountBarrier<T> {

    private AtomicInteger count = new AtomicInteger(0);
    private Consumer<List<T>> consumer;
    private BlockingQueue<T> queue = new LinkedBlockingQueue<>();

    public CountBarrier(int count, Consumer<List<T>> consumer) {
        this.count.set(count);
        this.consumer = consumer;
    }

    public void add(T item) {
        queue.add(item);
    }

    public void countDown() {
        int ret = count.decrementAndGet();
        if (ret <= 0) {
            List<T> collect = queue.stream().collect(Collectors.toList());
            consumer.accept(collect);
        }
    }
}
