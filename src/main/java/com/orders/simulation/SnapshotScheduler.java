package com.orders.simulation;

import com.orders.inventory.InventoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "snapshot-scheduler");
                t.setDaemon(true);
                return t;
            });

    private final InventoryManager inventoryManager;

    public SnapshotScheduler(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public void start(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            inventoryManager.persistSnapshot();
            log.info("Inventory snapshot persisted to DB.");
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);

        log.info("Snapshot scheduler started (every {}s).", intervalSeconds);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
