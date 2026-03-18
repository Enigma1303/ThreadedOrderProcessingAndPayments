package com.orders.config;

import com.orders.model.LoadType;

public class SystemConfig {

    public static final int QUEUE_CAPACITY = 500;

    public static final int WORKER_THREADS = 8;

    public static final double PAYMENT_SUCCESS_RATE = 0.80;
    public static final int PAYMENT_DELAY_MS_MIN    = 50;
    public static final int PAYMENT_DELAY_MS_MAX    = 200;

    public static final int SNAPSHOT_INTERVAL_SECONDS = 60;

    public static final int REPORT_INTERVAL_SECONDS = 3;

    public static final int RUN_DURATION_SECONDS = 30;


    public static LoadProfile getProfile(LoadType type)
    {
        switch (type)
        {
            case HIGH:  return new LoadProfile(8,  10,  0);
            case BURST: return new LoadProfile(3, 100, 50);
            default:    return new LoadProfile(3, 2000,  0);
        }
    }

    public static class LoadProfile
    {
        public final int producerCount;
        public final int delayMs;
        public final int burstSize;

        public LoadProfile(int producerCount, int delayMs, int burstSize) {
            this.producerCount = producerCount;
            this.delayMs       = delayMs;
            this.burstSize     = burstSize;
        }
    }
}
