package com.threadsdemo.advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PHASER & EXCHANGER DEMO — Specialized Synchronization
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 1. Phaser
 *    A more flexible, reusable version of both CountDownLatch and CyclicBarrier.
 *    It is "dynamic" — the number of participating threads (parties) can
 *    change at runtime via register() / arriveAndDeregister().
 *
 *    Use Case: Multi-phase parallel algorithm where the number of worker
 *    threads might fluctuate based on task complexity in each phase.
 *
 * 2. Exchanger
 *    A synchronization point where exactly two threads can swap objects.
 *
 *    Use Case: Dual-buffering. One thread fills a buffer with data while
 *    another thread empties it. When both are done, they "exchange" buffers
 *    and repeat, achieving continuous throughput.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class PhaserExchangerDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Phaser & Exchanger Demo ===\n");

        phaserDemo();
        System.out.println("═".repeat(70) + "\n");

        exchangerDemo();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 1. PHASER — Dynamic, Multi-Phase Synchronization
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Scenario: A 3-phase data-processing pipeline.
     *   Phase 0 — 4 workers parse raw data.
     *   Phase 1 — 1 worker drops out (data was trivial), 3 continue to validate.
     *   Phase 2 — 1 more worker drops out, 2 finalize the output.
     *
     * This shows Phaser's killer feature: dynamic party management.
     */
    static void phaserDemo() throws InterruptedException {
        System.out.println("--- Phaser: Dynamic Multi-Phase Pipeline ---\n");

        // Register the main thread so we can observe phase advancement
        Phaser phaser = new Phaser(1); // "1" = main thread is registered

        int initialWorkers = 4;
        Thread[] workers = new Thread[initialWorkers];

        for (int i = 0; i < initialWorkers; i++) {
            final int workerId = i + 1;
            phaser.register(); // dynamically add each worker

            workers[i] = new Thread(() -> {
                try {
                    // ── Phase 0: Parse ───────────────────────────
                    System.out.printf("  Worker-%d  Phase 0 — parsing …%n", workerId);
                    Thread.sleep(200 + ThreadLocalRandom.current().nextInt(300));
                    System.out.printf("  Worker-%d  Phase 0 ✓ done%n", workerId);
                    phaser.arriveAndAwaitAdvance();

                    // Workers 4 drops out after Phase 0
                    if (workerId == 4) {
                        System.out.printf("  Worker-%d  ↩ deregistering (data was trivial)%n", workerId);
                        phaser.arriveAndDeregister();
                        return;
                    }

                    // ── Phase 1: Validate ────────────────────────
                    System.out.printf("  Worker-%d  Phase 1 — validating …%n", workerId);
                    Thread.sleep(200 + ThreadLocalRandom.current().nextInt(300));
                    System.out.printf("  Worker-%d  Phase 1 ✓ done%n", workerId);
                    phaser.arriveAndAwaitAdvance();

                    // Worker 3 drops out after Phase 1
                    if (workerId == 3) {
                        System.out.printf("  Worker-%d  ↩ deregistering (validation complete)%n", workerId);
                        phaser.arriveAndDeregister();
                        return;
                    }

                    // ── Phase 2: Finalize ────────────────────────
                    System.out.printf("  Worker-%d  Phase 2 — finalizing …%n", workerId);
                    Thread.sleep(200 + ThreadLocalRandom.current().nextInt(300));
                    System.out.printf("  Worker-%d  Phase 2 ✓ done%n", workerId);
                    phaser.arriveAndDeregister();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + workerId);
            workers[i].start();
        }

        // Main thread observes each phase
        for (int phase = 0; phase < 3; phase++) {
            phaser.arriveAndAwaitAdvance();
            System.out.printf("%n  >>> Phase %d complete — registered parties: %d%n%n",
                    phase, phaser.getRegisteredParties());
        }

        // Main deregisters; phaser terminates when parties reach 0
        phaser.arriveAndDeregister();

        for (Thread w : workers) w.join();
        System.out.println("✔ Phaser pipeline finished. isTerminated = " + phaser.isTerminated() + "\n");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2. EXCHANGER — Dual-Buffer Swap
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Scenario: Dual-buffering.
     *   • The "Filler" thread fills a buffer with data items.
     *   • The "Drainer" thread processes / empties the buffer.
     *   • When both finish, they EXCHANGE buffers and repeat.
     *
     * This allows continuous throughput — one buffer is always being filled
     * while the other is being consumed.
     */
    static void exchangerDemo() throws InterruptedException {
        System.out.println("--- Exchanger: Dual-Buffer Swap ---\n");

        Exchanger<List<String>> exchanger = new Exchanger<>();

        final int ROUNDS = 3;

        // ── Filler thread ────────────────────────────────────────────────
        Thread filler = new Thread(() -> {
            List<String> buffer = new ArrayList<>();
            try {
                for (int round = 1; round <= ROUNDS; round++) {
                    // Fill the buffer
                    for (int i = 1; i <= 4; i++) {
                        String item = "R" + round + "-Item" + i;
                        buffer.add(item);
                        System.out.printf("  [Filler]   + added '%s'%n", item);
                        Thread.sleep(100);
                    }
                    System.out.printf("  [Filler]   buffer full (%d items) — exchanging …%n", buffer.size());

                    // Exchange: hand off the full buffer, receive an empty one
                    buffer = exchanger.exchange(buffer);

                    System.out.printf("  [Filler]   received empty buffer (size %d)%n%n", buffer.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Filler");

        // ── Drainer thread ───────────────────────────────────────────────
        Thread drainer = new Thread(() -> {
            List<String> buffer = new ArrayList<>(); // starts empty
            try {
                for (int round = 1; round <= ROUNDS; round++) {
                    System.out.printf("  [Drainer]  waiting for full buffer …%n");

                    // Exchange: hand off the empty buffer, receive the full one
                    buffer = exchanger.exchange(buffer);

                    System.out.printf("  [Drainer]  received full buffer (%d items) — processing …%n", buffer.size());
                    for (String item : buffer) {
                        System.out.printf("  [Drainer]  - processing '%s'%n", item);
                        Thread.sleep(80);
                    }
                    buffer.clear(); // empty it for the next round
                    System.out.printf("  [Drainer]  ✓ buffer drained%n%n");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Drainer");

        filler.start();
        drainer.start();

        filler.join();
        drainer.join();

        System.out.println("✔ Exchanger dual-buffer demo finished.\n");
    }
}

