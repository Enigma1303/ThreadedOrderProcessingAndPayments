package com.orders.producer;

import com.orders.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrderProducerPool {

    private static final Logger log = LoggerFactory.getLogger(OrderProducerPool.class);

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<Thread>  producerThreads = new ArrayList<>();

    public OrderProducerPool(int producerCount,
                             BlockingQueue<Order> queue,
                             int delayMs,
                             int burstSize) {

        for (int i = 0; i < producerCount; i++) {
            String id = "producer-" + i;
            OrderProducer producer = new OrderProducer(
                    id, queue, delayMs, burstSize, running
            );
            Thread t = new Thread(producer, id);
            t.setDaemon(false);
            producerThreads.add(t);
        }
    }

    public void start() {
        producerThreads.forEach(Thread::start);
        log.info("Started {} producer threads.", producerThreads.size());
    }

    public void shutdown() throws InterruptedException {
        log.info("Shutting down producers...");
        running.set(false);
        for (Thread t : producerThreads) {
            t.join(2000);
        }
        log.info("All producers shut down.");
    }
}