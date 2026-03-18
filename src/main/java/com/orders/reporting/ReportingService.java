package com.orders.reporting;

import com.orders.inventory.InventoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "reporting-scheduler");
                t.setDaemon(true);
                return t;
            });

    private final Metrics          metrics;
    private final InventoryManager inventoryManager;

    public ReportingService(InventoryManager inventoryManager) {
        this.metrics          = Metrics.getInstance();
        this.inventoryManager = inventoryManager;
    }

    public void start(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(
                this::printReport,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );
        log.info("Reporting service started (every {}s).", intervalSeconds);
    }

    private void printReport() {
        Map<String, Integer> stock = inventoryManager.getSnapshot();

        System.out.println("\n========== SYSTEM REPORT ==========");
        System.out.printf("  Total Processed  : %d%n", metrics.getTotalProcessed());
        System.out.printf("  Successful       : %d%n", metrics.getSuccessful());
        System.out.printf("  Failed           : %d%n", metrics.getFailed());
        System.out.printf("    Payment fails  : %d%n", metrics.getPaymentFailures());
        System.out.printf("    Stock fails    : %d%n", metrics.getStockFailures());
        System.out.println("  Inventory:");
        stock.forEach((pid, qty) ->
                System.out.printf("    %-6s → %d units%n", pid, qty));
        System.out.println("====================================\n");
    }

    public void printFinalReport() {
        System.out.println("\n========== FINAL REPORT ==========");
        printReport();
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
