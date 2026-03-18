package com.orders;

import com.orders.config.SystemConfig;
import com.orders.consumer.OrderConsumer;
import com.orders.db.DatabaseManager;
import com.orders.deadlock.DeadlockDemo;
import com.orders.inventory.InventoryManager;
import com.orders.model.LoadType;
import com.orders.model.Order;
import com.orders.payment.PaymentService;
import com.orders.producer.OrderProducer;
import com.orders.reporting.ReportingService;
import com.orders.simulation.SnapshotScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {


        LoadType loadType = args.length > 0
                ? LoadType.valueOf(args[0].toUpperCase())
                : LoadType.NORMAL;

        SystemConfig.LoadProfile profile = SystemConfig.getProfile(loadType);

       
        log.info("  THE ORDER PROCESSING SYSTEM IS STARTING");
        log.info("  Load Type  : {}", loadType);
        log.info("  Producers  : {}", profile.producerCount);
        log.info("  Delay      : {}ms", profile.delayMs);
        log.info("  Burst Size : {}", profile.burstSize);
        

        DatabaseManager  db  = DatabaseManager.getInstance();
        InventoryManager inv = new InventoryManager(db);
        PaymentService   pay = new PaymentService();
         
        System.out.println("\n-- Running Deadlock Simulation");
        try
      {
       DeadlockDemo.demonstratePrevention(inv);
      } 
      catch (InterruptedException e)
      {
       Thread.currentThread().interrupt();
      }
      System.out.println("Deadlock Demo Finished\n");
        SnapshotScheduler snapshot  = new SnapshotScheduler(inv);
        ReportingService  reporting = new ReportingService(inv);

        snapshot.start(SystemConfig.SNAPSHOT_INTERVAL_SECONDS);
        reporting.start(SystemConfig.REPORT_INTERVAL_SECONDS);

        BlockingQueue<Order> queue = new LinkedBlockingQueue<>(
                SystemConfig.QUEUE_CAPACITY);

        ExecutorService workerPool = Executors.newFixedThreadPool(
                SystemConfig.WORKER_THREADS,
                r -> {
                    Thread t = new Thread(r, "order-worker");
                    t.setDaemon(true);
                    return t;
                });

        OrderConsumer consumer = new OrderConsumer(
                queue,
                workerPool,
                pay,
                inv,
                db
        );
        Thread consumerThread = new Thread(consumer, "order-consumer");
        consumerThread.start();
        AtomicBoolean producersRunning = new AtomicBoolean(true);
        List<Thread>  producerThreads  = new ArrayList<>();

        for (int i = 1; i <= profile.producerCount; i++) {
            OrderProducer producer = new OrderProducer(
                    "Producer-" + i,
                    queue,
                    profile.delayMs,
                    profile.burstSize,
                    producersRunning
            );
            Thread t = new Thread(producer, "producer-" + i);
            t.start();
            producerThreads.add(t);
        }
        log.info("System running for {} seconds",
                SystemConfig.RUN_DURATION_SECONDS);

        Thread.sleep(TimeUnit.SECONDS.toMillis(
                SystemConfig.RUN_DURATION_SECONDS));

        log.info("Shutting down producers");
        producersRunning.set(false);
        for (Thread t : producerThreads) {
            t.join(2000);
        }

        log.info("Shutting down consumer");
        queue.offer(OrderConsumer.POISON_PILL);
        consumerThread.join(5000);

        log.info("Waiting for workers to finish");
        workerPool.shutdown();
        workerPool.awaitTermination(10, TimeUnit.SECONDS);

        inv.persistSnapshot();
        log.info("Final inventory snapshot saved.");

        reporting.printFinalReport();
        pay.shutdown();
        snapshot.shutdown();
        reporting.shutdown();

        log.info("System shutdown complete.");
    }
}