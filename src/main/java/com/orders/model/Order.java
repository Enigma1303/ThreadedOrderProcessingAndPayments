package com.orders.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {

    public enum Status {
        PENDING, SUCCESS, FAILED
    }

    private final String orderId;
    private final String productId;
    private final int quantity;
    private Status status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order(String productId, int quantity) {
        this.orderId   = UUID.randomUUID().toString();
        this.productId = productId;
        this.quantity  = quantity;
        this.status    = Status.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    } 
    public void setStatus(Status status) {
        this.status    = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }
    public String getProductId() {
        return productId;
    }
    public int getQuantity() {
        return quantity;
    }
    public Status getStatus() {
        return status;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

     @Override
    public String toString() {
        return String.format("Order{id='%s', product='%s', qty=%d, status=%s}",
                orderId.substring(0, 8), productId, quantity, status);
    }
}