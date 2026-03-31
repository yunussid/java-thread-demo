package com.threadsdemo.advanced;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * SYNCHRONIZATION UTILITIES DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Real-world demonstrations of core java.util.concurrent utilities:
 *
 * 1. Semaphore   — "The Traffic Controller"
 *    Throttling access to a legacy database that supports only N connections.
 *
 * 2. CountDownLatch — "The One-Time Gate"
 *    Main thread waits for multiple background services to report "Ready."
 *
 * 3. CyclicBarrier  — "The Reusable Meeting Point"
 *    Parallel processing of 1M records in chunks; threads aggregate results
 *    at a barrier before moving to the next processing phase.
 *
 * Key Distinction:
 *   • Semaphore has no ownership — any thread can release a permit.
 *   • CountDownLatch is one-shot — once the count reaches zero, it's done.
 *   • CyclicBarrier is reusable — it resets after all threads arrive.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class SynchronizationUtilitiesDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Synchronization Utilities Demo ===\n");

        semaphoreDemo();
        System.out.println("═".repeat(70) + "\n");

        countDownLatchDemo();
        System.out.println("═".repeat(70) + "\n");

        cyclicBarrierDemo();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 1. SEMAPHORE — The Traffic Controller
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Use Case: Your microservice talks to a legacy database that can only
     * handle 3 concurrent connections. A Semaphore with 3 permits prevents
     * the system from overwhelming the database.
     *
     * Key: Unlike a lock, a Semaphore has NO owner — any thread can release
     * a permit, even if it didn't acquire it.
     */
    static void semaphoreDemo() throws InterruptedException {
        System.out.println("--- Semaphore: The Traffic Controller ---");
        System.out.println("Legacy DB supports max 3 concurrent connections.");
        System.out.println("8 service threads compete for access.\n");

        final int MAX_DB_CONNECTIONS = 3;
        Semaphore connectionPool = new Semaphore(MAX_DB_CONNECTIONS, true); // fair

        Thread[] threads = new Thread[8];
        for (int i = 0; i < 8; i++) {
            final int serviceId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    System.out.printf("  Service-%d  → requesting DB connection …%n", serviceId);

                    connectionPool.acquire();  // blocks until a permit is available

                    System.out.printf("  Service-%d  ✓ CONNECTED  (available permits: %d)%n",
                            serviceId, connectionPool.availablePermits());

                    // simulate query execution
                    Thread.sleep(800 + ThreadLocalRandom.current().nextInt(400));

                    System.out.printf("  Service-%d  ← releasing connection%n", serviceId);
                    connectionPool.release();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Service-" + serviceId);
            threads[i].start();
            Thread.sleep(80);  // stagger start times for readable output
        }

        for (Thread t : threads) t.join();
        System.out.println("\n✔ All services finished their DB work.\n");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2. COUNT-DOWN LATCH — The One-Time Gate
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Use Case: Initializing a complex service. The main thread waits for
     * five background subsystems (DB, Kafka, Cache, Config, HealthCheck)
     * to report "Ready" before the application starts accepting traffic.
     *
     * A CountDownLatch is one-shot — once the count reaches zero, the gate
     * opens permanently and can never be reset.
     */
    static void countDownLatchDemo() throws InterruptedException {
        System.out.println("--- CountDownLatch: Service Initialization Gate ---");
        System.out.println("Main thread waits for 5 subsystems to become ready.\n");

        String[] subsystems = {"Database", "Kafka", "Redis-Cache", "Config-Server", "HealthCheck"};
        CountDownLatch readyLatch = new CountDownLatch(subsystems.length);

        for (String subsystem : subsystems) {
            new Thread(() -> {
                try {
                    int bootTimeMs = 300 + ThreadLocalRandom.current().nextInt(1200);
                    System.out.printf("  [%s] booting … (%d ms)%n", subsystem, bootTimeMs);
                    Thread.sleep(bootTimeMs);

                    readyLatch.countDown();
                    System.out.printf("  [%s] ✓ READY  (remaining: %d)%n",
                            subsystem, readyLatch.getCount());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, subsystem).start();
        }

        System.out.println("  ⏳ Main thread waiting for all subsystems …\n");
        readyLatch.await();  // blocks until count == 0

        System.out.println("\n  🚀 ALL SUBSYSTEMS READY — Application is now accepting traffic!\n");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 3. CYCLIC BARRIER — The Reusable Meeting Point
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Use Case: Processing 1,000,000 records in 4 chunks (250k each).
     * Each worker thread processes its chunk independently, then all threads
     * wait at the barrier so we can aggregate partial results before moving
     * to the next processing phase.
     *
     * CyclicBarrier is reusable — it resets automatically after all parties
     * arrive, so it works naturally for multi-phase pipelines.
     */
    static void cyclicBarrierDemo() throws InterruptedException {
        System.out.println("--- CyclicBarrier: Parallel Chunk Processing ---");
        System.out.println("Processing 1,000,000 records in 4 chunks across 2 phases.\n");

        final int TOTAL_RECORDS = 1_000_000;
        final int CHUNK_COUNT = 4;
        final int CHUNK_SIZE = TOTAL_RECORDS / CHUNK_COUNT;

        // shared partial results for aggregation
        int[] partialCounts = new int[CHUNK_COUNT];
        AtomicInteger phaseNumber = new AtomicInteger(1);

        // Barrier action runs once, by the last thread to arrive
        CyclicBarrier barrier = new CyclicBarrier(CHUNK_COUNT, () -> {
            int total = 0;
            for (int c : partialCounts) total += c;
            System.out.printf("  >>> BARRIER — Phase %d complete.  Aggregated count: %,d%n%n",
                    phaseNumber.getAndIncrement(), total);
        });

        Thread[] workers = new Thread[CHUNK_COUNT];
        for (int i = 0; i < CHUNK_COUNT; i++) {
            final int chunkId = i;
            final int startRecord = chunkId * CHUNK_SIZE + 1;
            final int endRecord = startRecord + CHUNK_SIZE - 1;

            workers[i] = new Thread(() -> {
                try {
                    // ── Phase 1: Filter records ──
                    System.out.printf("  Chunk-%d  Phase 1 — filtering records %,d … %,d%n",
                            chunkId, startRecord, endRecord);
                    Thread.sleep(200 + ThreadLocalRandom.current().nextInt(600));
                    int filtered = CHUNK_SIZE / 2 + ThreadLocalRandom.current().nextInt(1000);
                    partialCounts[chunkId] = filtered;
                    System.out.printf("  Chunk-%d  Phase 1 done — %,d records passed filter%n",
                            chunkId, filtered);
                    barrier.await();  // wait for all chunks → aggregation runs

                    // ── Phase 2: Transform records ──
                    System.out.printf("  Chunk-%d  Phase 2 — transforming %,d records%n",
                            chunkId, partialCounts[chunkId]);
                    Thread.sleep(150 + ThreadLocalRandom.current().nextInt(400));
                    int transformed = partialCounts[chunkId] - ThreadLocalRandom.current().nextInt(500);
                    partialCounts[chunkId] = transformed;
                    System.out.printf("  Chunk-%d  Phase 2 done — %,d records transformed%n",
                            chunkId, transformed);
                    barrier.await();  // barrier is reusable!

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }, "Chunk-" + chunkId);
            workers[i].start();
        }

        for (Thread w : workers) w.join();
        System.out.println("✔ All chunks processed across both phases.\n");
    }
}
