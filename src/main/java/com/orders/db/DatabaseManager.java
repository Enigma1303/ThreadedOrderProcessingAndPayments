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
        verifyConnection();
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    private void verifyConnection() {
        try (Connection conn = getConnection()) {
            log.info("Database connection verified successfully.");
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Cannot connect to database. Is Docker running?", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
    }
}