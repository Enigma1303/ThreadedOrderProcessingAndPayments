package com.orders;

import com.orders.config.SystemConfig;
import com.orders.consumer.OrderConsumerPool;
import com.orders.db.DatabaseManager;
import com.orders.deadlock.DeadlockDemo;
import com.orders.inventory.InventoryManager;
import com.orders.model.LoadType;
import com.orders.model.Order;
import com.orders.payment.PaymentService;
import com.orders.producer.OrderProducerPool;
import com.orders.reporting.ReportingService;
import com.orders.simulation.SnapshotScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final int CONSUMER_COUNT = 3;

    public static void main(String[] args) throws InterruptedException {

        LoadType loadType = args.length > 0
                ? LoadType.valueOf(args[0].toUpperCase())
                : LoadType.NORMAL;

        SystemConfig.LoadProfile profile = SystemConfig.getProfile(loadType);

        log.info("THE ORDER PROCESSING SYSTEM IS STARTING");
        log.info("Load Type   : {}", loadType);
        log.info("Producers   : {}", profile.producerCount);
        log.info("Consumers   : {}", CONSUMER_COUNT);
        log.info("Delay       : {}ms", profile.delayMs);
        log.info("Burst Size  : {}", profile.burstSize);

       
        DatabaseManager  db  = DatabaseManager.getInstance();
        InventoryManager inv = new InventoryManager(db);
        PaymentService   pay = new PaymentService();

      
        System.out.println("\n-- Running Deadlock Prevention Demo");
        try {
            DeadlockDemo.demonstratePrevention(inv);
        } catch (InterruptedException e) {
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

      
        OrderConsumerPool consumerPool = new OrderConsumerPool(
                CONSUMER_COUNT, queue, workerPool, pay, inv, db
        );
        consumerPool.start();

      
        OrderProducerPool producerPool = new OrderProducerPool(
                profile.producerCount,
                queue,
                profile.delayMs,
                profile.burstSize
        );
        producerPool.start();

       
        log.info("System running for {} seconds", SystemConfig.RUN_DURATION_SECONDS);
        Thread.sleep(TimeUnit.SECONDS.toMillis(SystemConfig.RUN_DURATION_SECONDS));

     
        log.info("Shutting down producers...");
        producerPool.shutdown();       

        log.info("Shutting down consumers...");
        consumerPool.shutdown();        

        log.info("Waiting for workers to finish...");
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