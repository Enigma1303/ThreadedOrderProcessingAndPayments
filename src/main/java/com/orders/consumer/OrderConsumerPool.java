package com.orders.consumer;

import com.orders.db.DatabaseManager;
import com.orders.inventory.InventoryManager;
import com.orders.model.Order;
import com.orders.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class OrderConsumerPool {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumerPool.class);

    private final int                  consumerCount;
    private final BlockingQueue<Order> queue;
    private final List<Thread>         consumerThreads = new ArrayList<>();

    public OrderConsumerPool(int consumerCount,
                             BlockingQueue<Order> queue,
                             ExecutorService workerPool,
                             PaymentService paymentService,
                             InventoryManager inventoryManager,
                             DatabaseManager db) {
        this.consumerCount = consumerCount;
        this.queue         = queue;

        for (int i = 0; i < consumerCount; i++) {
            String id = "consumer-" + i;
            OrderConsumer consumer = new OrderConsumer(
                    id, queue, workerPool, paymentService, inventoryManager, db
            );
            Thread t = new Thread(consumer, id);
            t.setDaemon(false);
            consumerThreads.add(t);
        }
    }

    public void start() {
        consumerThreads.forEach(Thread::start);
        log.info("Started {} consumer threads.", consumerCount);
    }

   
    public void shutdown() throws InterruptedException {
        log.info("Sending {} poison pills...", consumerCount);
        for (int i = 0; i < consumerCount; i++) {
            queue.put(OrderConsumer.POISON_PILL);
        }
        for (Thread t : consumerThreads) {
            t.join();
        }
        log.info("All consumers shut down.");
    }
}