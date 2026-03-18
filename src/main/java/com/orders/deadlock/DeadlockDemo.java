package com.orders.deadlock;

import com.orders.inventory.InventoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

public class DeadlockDemo {

    private static final Logger log = LoggerFactory.getLogger(DeadlockDemo.class);

   
    // DEADLOCK SCENARIO - shows how deadlock happens will hang if actually ran 
    
    
    /*
    public static void causeDeadlock(InventoryManager inv) throws InterruptedException {

        Thread t1 = new Thread(() -> {
            ReentrantLock lockA = inv.getLockForDemo("P001");
            ReentrantLock lockB = inv.getLockForDemo("P002");

            lockA.lock();  // Thread1 acquires P001
            log.info("[Thread-1] Acquired lock on P001");

            try {

                Thread.sleep(50); 

                log.info("[Thread-1] Waiting for P002...");


                 // Thread1 wants P002 - but Thread2 has it
                lockB.lock();    
                
               
                log.info("[Thread-1] Acquired lock on P002 - will never print");

            } catch (InterruptedException e)
             {
                Thread.currentThread().interrupt();
            } 
            finally
            {
                lockB.unlock();
                lockA.unlock();
            }
        }, "deadlock-thread-1");

        Thread t2 = new Thread(() -> {
            ReentrantLock lockA = inv.getLockForDemo("P001");
            ReentrantLock lockB = inv.getLockForDemo("P002");

            lockB.lock();  // Thread2 acquires P002
            log.info("[Thread-2] Acquired lock on P002");

            try {
                Thread.sleep(50);

                log.info("[Thread-2] Waiting for P001...");
                lockA.lock();      
                log.info("[Thread-2] Acquired lock on P001 - will never print");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lockA.unlock();
                lockB.unlock();
            }
        }, "deadlock-thread-2");

        t1.start();
        t2.start();

        //makes it hang forever because of deadlock
        t1.join();
        t2.join();  
    */



    public static void demonstratePrevention(InventoryManager inv)
            throws InterruptedException {

        log.info("=== Deadlock Prevention Demo Starting ===");
        log.info("Both threads want P001 and P002");
        log.info("Prevention:always acquire locks in ascending order");

        Thread t1 = new Thread(() -> {
            // passes P002 first but sort will reorder to P001, P002
            ReentrantLock[] locks = inv.acquireLocksOrdered("P002", "P001");
            try {
                log.info("[Thread-1] Acquired locks on P001 + P002 (sorted order)");
                Thread.sleep(100);
                log.info("[Thread-1] Work done, releasing locks");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                inv.releaseLocks(locks);
            }
        }, "demo-thread-1");

        Thread t2 = new Thread(() -> {
          
            ReentrantLock[] locks = inv.acquireLocksOrdered("P001", "P002");
            try {
                log.info("[Thread-2] Acquired locks on P001 + P002 (sorted order)");
                Thread.sleep(100);
                log.info("[Thread-2] Work done, releasing locks");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                inv.releaseLocks(locks);
            }
        }, "demo-thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        log.info("=== Deadlock Prevention Demo Complete - No Deadlock! ===");
    }
}