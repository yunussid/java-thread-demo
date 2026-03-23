package com.threadsdemo.advanced;

import java.util.concurrent.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SYNCHRONIZATION UTILITIES DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Java provides utility classes for thread coordination:
 * - Semaphore: Limit concurrent access to a resource
 * - CountDownLatch: Wait for N events (one-time)
 * - CyclicBarrier: Synchronize N threads (reusable)
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class SynchronizationUtilitiesDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Synchronization Utilities Demo ===\n");

        semaphoreDemo();
        countDownLatchDemo();
        cyclicBarrierDemo();
    }

    /**
     * Semaphore - Limit concurrent access
     */
    static void semaphoreDemo() throws InterruptedException {
        System.out.println("--- Semaphore Demo ---");
        System.out.println("Simulating connection pool with max 3 connections\n");

        // Only 3 permits available (like 3 database connections)
        Semaphore connectionPool = new Semaphore(3);

        // Create 8 tasks wanting to use connections
        Thread[] threads = new Thread[8];
        for (int i = 0; i < 8; i++) {
            final int userId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.println("User " + userId + " waiting for connection...");

                    connectionPool.acquire();  // Wait for permit

                    System.out.println("User " + userId + " GOT connection! " +
                        "(Available: " + connectionPool.availablePermits() + ")");

                    // Use the connection
                    Thread.sleep(1000);

                    System.out.println("User " + userId + " releasing connection");
                    connectionPool.release();  // Release permit

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "User-" + userId);
            threads[i].start();
            Thread.sleep(100);  // Stagger start times
        }

        for (Thread t : threads) t.join();
        System.out.println("\nAll users done!\n");
    }

    /**
     * CountDownLatch - Wait for N events to complete
     */
    static void countDownLatchDemo() throws InterruptedException {
        System.out.println("--- CountDownLatch Demo ---");
        System.out.println("Main thread waiting for 3 workers to complete\n");

        int workerCount = 3;
        CountDownLatch latch = new CountDownLatch(workerCount);

        for (int i = 1; i <= workerCount; i++) {
            final int workerId = i;
            new Thread(() -> {
                try {
                    System.out.println("Worker " + workerId + " started");
                    Thread.sleep(workerId * 500);  // Different completion times
                    System.out.println("Worker " + workerId + " finished");

                    latch.countDown();  // Decrement count
                    System.out.println("Latch count: " + latch.getCount());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + workerId).start();
        }

        System.out.println("Main thread waiting...");
        latch.await();  // Block until count reaches 0
        System.out.println("All workers completed! Main thread continues.\n");
    }

    /**
     * CyclicBarrier - Threads wait for each other
     */
    static void cyclicBarrierDemo() throws InterruptedException {
        System.out.println("--- CyclicBarrier Demo ---");
        System.out.println("3 threads doing 2 phases, waiting for each other at barriers\n");

        int partySize = 3;

        // Barrier with action when all threads arrive
        CyclicBarrier barrier = new CyclicBarrier(partySize, () -> {
            System.out.println(">>> All threads reached barrier! Proceeding to next phase...\n");
        });

        Thread[] threads = new Thread[partySize];
        for (int i = 0; i < partySize; i++) {
            final int threadId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    // Phase 1
                    System.out.println("Thread " + threadId + " doing Phase 1...");
                    Thread.sleep(threadId * 300);
                    System.out.println("Thread " + threadId + " waiting at barrier (Phase 1 done)");
                    barrier.await();  // Wait for all

                    // Phase 2
                    System.out.println("Thread " + threadId + " doing Phase 2...");
                    Thread.sleep(threadId * 200);
                    System.out.println("Thread " + threadId + " waiting at barrier (Phase 2 done)");
                    barrier.await();  // Barrier is reusable!

                    System.out.println("Thread " + threadId + " completed all phases!");

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }, "Thread-" + threadId);
            threads[i].start();
        }

        for (Thread t : threads) t.join();
        System.out.println("\nAll threads finished!\n");
    }
}
