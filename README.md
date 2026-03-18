# Concurrent Order Processing System

A multi-threaded e-commerce backend simulation built in Java, demonstrating concurrent order processing, thread-safe inventory management, and async payment handling.

## Tech Stack

- Java 21
- Maven
- MySQL 8.0 (Docker)
- SLF4J + Logback


## Prerequisites

- Java 21
- Maven
- Docker

## Setup

**1. Clone the repo**
```bash
git clone https://github.com/Enigma1303/ThreadedOrderProcessingAndPayments.git
cd ThreadedOrderProcessingAndPayments
```

**2. Start MySQL**
```bash
docker compose up -d
```

**3. Create database user**
```bash
docker exec -it orders-mysql mysql -u root -proot
```
```sql
CREATE USER IF NOT EXISTS 'orders'@'%' IDENTIFIED BY 'orders123';
GRANT ALL PRIVILEGES ON orderdb.* TO 'orders'@'%';
FLUSH PRIVILEGES;
EXIT;
```

**4. Make run script executable**
```bash
chmod +x run.sh
```

## Running
```bash
# Normal load - 1 producer, 2 second delay
./run.sh NORMAL

# High load - 8 producers, 10ms delay
./run.sh HIGH

# Burst load - 50 orders instantly then steady
./run.sh BURST
```

The script automatically resets the database before each run.

## Architecture

### Producer Consumer Pattern
```
Producers → BlockingQueue → Consumer → ExecutorService → OrderProcessor
```

Multiple producer threads generate orders and push them into a shared `LinkedBlockingQueue`. A single consumer thread drains the queue and submits each order to a fixed thread pool for concurrent processing.

### Order Processing Flow
```
Order received
    ↓
Insert to DB as PENDING
    ↓
Async payment (Callable + Future)
    ↓
Payment failed?   → mark FAILED, update DB
Payment success?  → reserve stock
    ↓
Stock unavailable? → mark FAILED, update DB
Stock available?   → mark SUCCESS, update DB
```

### Inventory Management
Inventory is loaded from MySQL into a `ConcurrentHashMap` at startup. All stock operations run against this in-memory map for performance. A `ScheduledExecutorService` persists the map back to MySQL every 10 seconds as a snapshot.

Thread safety is enforced using `ReentrantLock` at the product level — one lock per product ID. This means threads processing different products run simultaneously, only blocking when two threads compete for the same product.

### Async Payment
Each payment is submitted as a `Callable<Boolean>` to a dedicated payment thread pool, returning a `Future<Boolean>` immediately. The worker thread calls `future.get()` to wait for the result, allowing payment processing to happen in parallel across all active orders.

### Metrics
Order counters use `AtomicInteger` for lock-free thread-safe tracking across all worker threads.

## Deadlock Scenario and Prevention

### How a Deadlock Can Occur
When two threads need to lock multiple products in opposite order:
```
Thread1: lock(P001) → waiting for lock(P002)
Thread2: lock(P002) → waiting for lock(P001)
→ Both waiting forever = deadlock
```

### Prevention — Sorted Lock Ordering
All multi-product lock acquisitions sort product IDs in ascending order before locking:
```java
public ReentrantLock[] acquireLocksOrdered(String... productIds) {
    Arrays.sort(productIds);  // always P001 before P002
    // lock in sorted order
}
```

Result:
```
Thread1: sort → lock P001 → lock P002
Thread2: sort → lock P001 → BLOCKED (waits for Thread1)
Thread1 finishes → releases → Thread2 proceeds
→ No circular wait, no deadlock
```

### Other Deadlock Prevention Practices
- Keep critical sections short — no payment processing inside locks
- Minimize nested lock usage — prefer single product operations
- Always release locks in `finally` blocks

## Load Profiles

| Mode   | Producers | Delay   | Burst |
|--------|-----------|---------|-------|
| NORMAL | 1         | 2000ms  | 0     |
| HIGH   | 8         | 10ms    | 0     |
| BURST  | 3         | 100ms   | 50    |

## Sample Output
```
========== SYSTEM REPORT ==========
  Total Processed  : 45
  Successful       : 32
  Failed           : 13
    Payment fails  : 5
    Stock fails    : 8
  Inventory:
    P001   → 67 units
    P002   → 54 units
    P003   → 41 units
    P004   → 98 units
    P005   → 23 units
====================================
```

## Configuration

All system settings are in `SystemConfig.java`:

| Setting | Default | Description |
|---------|---------|-------------|
| WORKER_THREADS | 8 | Order processing thread pool size |
| QUEUE_CAPACITY | 500 | Max orders in queue |
| PAYMENT_SUCCESS_RATE | 0.80 | 80% payments succeed |
| SNAPSHOT_INTERVAL_SECONDS | 10 | How often inventory saves to DB |
| REPORT_INTERVAL_SECONDS | 3 | How often stats print |
| RUN_DURATION_SECONDS | 30 | Simulation duration |