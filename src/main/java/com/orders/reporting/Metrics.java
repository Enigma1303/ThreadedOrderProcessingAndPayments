package com.orders.reporting;

import java.util.concurrent.atomic.AtomicInteger;

public class Metrics {
    private static final Metrics INSTANCE = new Metrics();

    private Metrics() {}

    public static Metrics getInstance() {
        return INSTANCE;
    }

    private final AtomicInteger totalProcessed   = new AtomicInteger(0);
    private final AtomicInteger successfulOrders = new AtomicInteger(0);
    private final AtomicInteger failedOrders     = new AtomicInteger(0);
    private final AtomicInteger paymentFailures  = new AtomicInteger(0);
    private final AtomicInteger stockFailures    = new AtomicInteger(0);

    public void recordSuccess()
    {
        totalProcessed.incrementAndGet();
        successfulOrders.incrementAndGet();
    }

    public void recordPaymentFailure()
    {
        totalProcessed.incrementAndGet();
        failedOrders.incrementAndGet();
        paymentFailures.incrementAndGet();
    }

    public void recordStockFailure() {
        totalProcessed.incrementAndGet();
        failedOrders.incrementAndGet();
        stockFailures.incrementAndGet();
    }
    public int getTotalProcessed()  { return totalProcessed.get(); }
    public int getSuccessful()      { return successfulOrders.get(); }
    public int getFailed()          { return failedOrders.get(); }
    public int getPaymentFailures() { return paymentFailures.get(); }
    public int getStockFailures()   { return stockFailures.get(); }
}

