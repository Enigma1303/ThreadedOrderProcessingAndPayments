package com.orders.payment;

import com.orders.config.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Random random = new Random();


    //r-> implements thread facory and creates daemon(auto exit,) threads named "payment-worker"
    private final ExecutorService paymentPool = Executors.newFixedThreadPool(4,
            r -> {
                Thread t = new Thread(r, "payment-worker");
                t.setDaemon(true);
                return t;
            });

    public Future<Boolean> processPayment(String orderId, double amount) {

        Callable<Boolean> paymentTask = () -> {

            int delay = SystemConfig.PAYMENT_DELAY_MS_MIN +
                    random.nextInt(SystemConfig.PAYMENT_DELAY_MS_MAX
                            - SystemConfig.PAYMENT_DELAY_MS_MIN);
            Thread.sleep(delay);
            boolean success = random.nextDouble() < SystemConfig.PAYMENT_SUCCESS_RATE;

            log.info("Payment for order {} -> {} ({}ms)",
                    orderId.substring(0, 8),
                    success ? "SUCCESS" : "FAILED",
                    delay);

            return success;
        };
        return paymentPool.submit(paymentTask);
    }

    public void shutdown() {
        paymentPool.shutdown();
    }
}
