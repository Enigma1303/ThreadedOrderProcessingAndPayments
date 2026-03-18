package com.orders.consumer;

import com.orders.db.DatabaseManager;
import com.orders.inventory.InventoryManager;
import com.orders.model.Order;
import com.orders.payment.PaymentService;
import com.orders.reporting.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.Future;

public class OrderProcessor implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    private final Order order;
    private final PaymentService  paymentService;
    private final InventoryManager inventoryManager;
    private final DatabaseManager  db;
    private final Metrics metrics = Metrics.getInstance();

    public OrderProcessor(Order order,
                          PaymentService paymentService,
                          InventoryManager inventoryManager,
                          DatabaseManager db) {
        this.order            = order;
        this.paymentService   = paymentService;
        this.inventoryManager = inventoryManager;
        this.db               = db;
    }

    @Override
    public void run() {
        log.info("Processing {}", order);
        insertOrder(order);
        Future<Boolean> paymentFuture = paymentService
                .processPayment(order.getOrderId(), order.getQuantity() * 10.0);

        boolean paymentSuccess;
        try {
            paymentSuccess = paymentFuture.get();
        } catch (Exception e) {
            log.error("Payment exception for order {}", order.getOrderId(), e);
            paymentSuccess = false;
        }
        if (!paymentSuccess)
        {
            order.setStatus(Order.Status.FAILED);
            updateOrderStatus(order.getOrderId(), Order.Status.FAILED);
            metrics.recordPaymentFailure();
            log.info("PAYMENT FAILED → {}", order);
            return;
        }

        boolean stockReserved = inventoryManager
                .reserveStock(order.getProductId(), order.getQuantity());

        if (stockReserved) {
            order.setStatus(Order.Status.SUCCESS);
            updateOrderStatus(order.getOrderId(), Order.Status.SUCCESS);
            metrics.recordSuccess();
            log.info("ORDER SUCCESS → {}", order);
        } else {
            order.setStatus(Order.Status.FAILED);
            updateOrderStatus(order.getOrderId(), Order.Status.FAILED);
            metrics.recordStockFailure();
            log.info("STOCK FAILURE → {}", order);
        }
    }
    private void insertOrder(Order order) {
        String sql = "INSERT INTO orders " +
                "(order_id, product_id, quantity, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getOrderId());
            ps.setString(2, order.getProductId());
            ps.setInt(3, order.getQuantity());
            ps.setString(4, order.getStatus().name());
            ps.setTimestamp(5, Timestamp.valueOf(order.getCreatedAt()));
            ps.setTimestamp(6, Timestamp.valueOf(order.getUpdatedAt()));
            ps.executeUpdate();
        } 
        catch (SQLException e)
        {
            log.error("Failed to insert order {}", order.getOrderId(), e);
        }
    }

    private void updateOrderStatus(String orderId, Order.Status status) {
        String sql = "UPDATE orders SET status=?, updated_at=? WHERE order_id=?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, orderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to update order {}", orderId, e);
        }
    }
}
