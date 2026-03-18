package com.orders.consumer;

import com.orders.db.DatabaseManager;
import com.orders.inventory.InventoryManager;
import com.orders.model.Order;
import com.orders.payment.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    public static final Order POISON_PILL = new Order("__POISON__", 0);

    private final BlockingQueue<Order> queue;
    private final ExecutorService      workerPool;
    private final PaymentService       paymentService;
    private final InventoryManager     inventoryManager;
    private final DatabaseManager      db;

    private volatile boolean running = true;

    public OrderConsumer(BlockingQueue<Order> queue,
                         ExecutorService workerPool,
                         PaymentService paymentService,
                         InventoryManager inventoryManager,
                         DatabaseManager db) {
        this.queue            = queue;
        this.workerPool       = workerPool;
        this.paymentService   = paymentService;
        this.inventoryManager = inventoryManager;
        this.db               = db;
    }

    @Override
    public void run() {
        log.info("OrderConsumer started.");

        while (running) {
            try {
                Order order = queue.poll(500, TimeUnit.MILLISECONDS);
                if (order == null) continue;
                if (order.getProductId().equals(POISON_PILL.getProductId())) {
                    log.info("Poison pill received - consumer shutting down.");
                    running = false;
                    break;
                }
                workerPool.submit(new OrderProcessor(
                        order,
                        paymentService,
                        inventoryManager,
                        db
                ));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }

        log.info("OrderConsumer stopped.");
    }

    public void stop() {
        running = false;
    }
}
