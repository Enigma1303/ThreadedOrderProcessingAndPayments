-- Drop and recreate for clean state
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS inventory;

-- Inventory table
CREATE TABLE inventory (
    product_id      VARCHAR(20) PRIMARY KEY,
    available_stock INT         NOT NULL
);

-- Orders table
CREATE TABLE orders (
    order_id   VARCHAR(36)  PRIMARY KEY,
    product_id VARCHAR(20)  NOT NULL,
    quantity   INT          NOT NULL,
    status     VARCHAR(10)  NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

-- Seed inventory
INSERT INTO inventory (product_id, available_stock) VALUES
('P001', 100),
('P002', 80),
('P003', 60),
('P004', 120),
('P005', 50);