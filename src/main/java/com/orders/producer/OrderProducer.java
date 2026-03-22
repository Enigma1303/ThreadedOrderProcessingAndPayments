package com.orders.producer;

import com.orders.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;

public class OrderProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private static final String[] PRODUCTS = {"P001", "P002", "P003", "P004", "P005"};

    //making it thread safe for multiple producers to generate orders with random products and quantities

    //private static final Random   RANDOM   = new Random();

    private final String name;
    private final BlockingQueue<Order> queue;
    private final int delayMs; 
    private final int  burstSize;
    private final AtomicBoolean running;

    public OrderProducer(String name,
                         BlockingQueue<Order> queue,
                         int delayMs,
                         int burstSize,
                         AtomicBoolean running) {
        this.name      = name;
        this.queue     = queue;
        this.delayMs   = delayMs;
        this.burstSize = burstSize;
        this.running   = running;
    }

    @Override
    public void run() {
        log.info("{} started (delay={}ms, burst={})", name, delayMs, burstSize);

        for (int i = 0; i < burstSize && running.get(); i++)
        {
            enqueue(createOrder());
        }

        while (running.get()) {
            try {
                enqueue(createOrder());
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("{} stopped.", name);
    }

    private void enqueue(Order order) {
        try {
            boolean accepted = queue.offer(order, 1, TimeUnit.SECONDS);
            if (!accepted)
            {
                log.warn("{}: queue full, order dropped.", name);
            } else
            {
                log.info("{}: enqueued {}", name, order);
            }
        }
         catch (InterruptedException e)
         {
            Thread.currentThread().interrupt();
        }
    }
    
    //using thread local random to avoid contention between multiple producer threads when generating random products and quantities
    private Order createOrder() {
    String product  = PRODUCTS[ThreadLocalRandom.current().nextInt(PRODUCTS.length)];
    int quantity = 1 + ThreadLocalRandom.current().nextInt(5);
    return new Order(product, quantity);
}
}
