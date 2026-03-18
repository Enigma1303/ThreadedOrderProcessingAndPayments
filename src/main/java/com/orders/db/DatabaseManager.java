package com.orders.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/orderdb" +
            "?useSSL=false" +
            "&allowPublicKeyRetrieval=true" +
            "&serverTimezone=UTC";

    private static final String DB_USER = "orders";
    private static final String DB_PASS = "orders123";
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private DatabaseManager() {
        initSchema();
        seedInventory();
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
    }

    private void initSchema() {
        String createOrders =
                "CREATE TABLE IF NOT EXISTS orders (" +
                "order_id   VARCHAR(36)  PRIMARY KEY, " +
                "product_id VARCHAR(20)  NOT NULL, " +
                "quantity   INT          NOT NULL, " +
                "status     VARCHAR(10)  NOT NULL, " +
                "created_at TIMESTAMP    NOT NULL, " +
                "updated_at TIMESTAMP    NOT NULL)";

        String createInventory =
                "CREATE TABLE IF NOT EXISTS inventory (" +
                "product_id      VARCHAR(20) PRIMARY KEY, " +
                "available_stock INT         NOT NULL)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createOrders);
            stmt.execute(createInventory);
            log.info("Schema initialised.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise schema", e);
        }
    }


    private void seedInventory() {
        String check  = "SELECT COUNT(*) FROM inventory";
        String insert = "INSERT IGNORE INTO inventory (product_id, available_stock) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(check)) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    try (PreparedStatement ps = conn.prepareStatement(insert)) {
                        String[][] products = {
                            {"P001", "100"},
                            {"P002", "80"},
                            {"P003", "60"},
                            {"P004", "120"},
                            {"P005", "50"}
                        };
                        for (String[] p : products) {
                            ps.setString(1, p[0]);
                            ps.setInt(2, Integer.parseInt(p[1]));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                        log.info("Inventory seeded with 5 products.");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed inventory", e);
        }
    }
}
