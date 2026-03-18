package com.orders.inventory;

import com.orders.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class InventoryManager {

    private static final Logger log = LoggerFactory.getLogger(InventoryManager.class);

    private final Map<String, Integer> inventory;

    private final Map<String, ReentrantLock> productLocks;

    private final DatabaseManager db;

    public InventoryManager(DatabaseManager db)
    {
        this.db           = db;
        this.inventory    = new ConcurrentHashMap<>(loadFromDb());
        this.productLocks = new ConcurrentHashMap<>();

        inventory.keySet().forEach(productId ->
                productLocks.put(productId, new ReentrantLock()));

        log.info("InventoryManager ready. Stock: {}", inventory);
    }


    private Map<String, Integer> loadFromDb()
    {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        String sql = "SELECT product_id, available_stock FROM inventory";

        try (Connection conn = db.getConnection();
             Statement st   = conn.createStatement();
             ResultSet rs   = st.executeQuery(sql)) {
            while (rs.next())
            {
                map.put(rs.getString("product_id"), rs.getInt("available_stock"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load inventory from DB", e);
        }

        log.info("Loaded {} products from database.", map.size());
        return map;
    }

    public boolean reserveStock(String productId, int quantity)
    {

        ReentrantLock lock = getLock(productId);
        lock.lock();

        try {
            int current = inventory.getOrDefault(productId, 0);

            if (current >= quantity) {
                inventory.put(productId, current - quantity);
                log.info("Reserved {} units of {}. Remaining: {}",
                        quantity, productId, current - quantity);
                return true;
            } else {
                log.warn("Insufficient stock for {}. Have: {}, Need: {}",
                        productId, current, quantity);
                return false;
            }
        } finally
        {

            lock.unlock();
        }
    }

    public ReentrantLock[] acquireLocksOrdered(String... productIds)
    {

        java.util.Arrays.sort(productIds);

        ReentrantLock[] locks = new ReentrantLock[productIds.length];
        for (int i = 0; i < productIds.length; i++) {
            locks[i] = getLock(productIds[i]);
            locks[i].lock();
        }
        return locks;
    }

    public void releaseLocks(ReentrantLock[] locks) {
        for (int i = locks.length - 1; i >= 0; i--) {
            locks[i].unlock();
        }
    }

    public void persistSnapshot() {
        String sql = "UPDATE inventory SET available_stock = ? WHERE product_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                ps.setInt(1, entry.getValue());
                ps.setString(2, entry.getKey());
                ps.addBatch();
            }
            ps.executeBatch();
            log.info("Inventory snapshot persisted to DB.");
        } catch (SQLException e) {
            log.error("Failed to persist snapshot", e);
        }
    }

    public int getStock(String productId) {
        return inventory.getOrDefault(productId, 0);
    }

    public Map<String, Integer> getSnapshot() {
        return Collections.unmodifiableMap(inventory);
    }
    private ReentrantLock getLock(String productId) {
        return productLocks.computeIfAbsent(productId, k -> new ReentrantLock());
    }

    public ReentrantLock getLockForDemo(String productId) {
    return getLock(productId);
}
}
