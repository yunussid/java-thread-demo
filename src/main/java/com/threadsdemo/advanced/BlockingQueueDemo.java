package com.threadsdemo.advanced;

import java.util.concurrent.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * BLOCKING QUEUE DEMO — The Producer-Consumer Backbone
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * BlockingQueue is the most critical utility for decoupled systems.
 * It is a thread-safe queue that:
 *   • Blocks the PRODUCER if the queue is full.
 *   • Blocks the CONSUMER if the queue is empty.
 *
 * Use Case — Asynchronous Order Processing:
 *   Your API receives "Order" objects at a very high rate during traffic
 *   spikes. Instead of processing them inline (which would blow up latency),
 *   the API thread puts orders into a LinkedBlockingQueue, and a separate
 *   pool of worker threads consumes them at their own pace.
 *   This is essential for maintaining high TPS during peak traffic.
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class BlockingQueueDemo {

    // Bounded queue — simulates back-pressure when capacity is reached
    private static final int QUEUE_CAPACITY = 5;
    private static final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    // Poison pill to signal consumers to shut down gracefully
    private static final Order POISON_PILL = new Order(-1, "SHUTDOWN");

    // ── Simple Order record ──────────────────────────────────────────────
    record Order(int id, String item) {
        @Override
        public String toString() {
            return "Order{id=" + id + ", item='" + item + "'}";
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== BlockingQueue: Producer-Consumer Demo ===\n");
        System.out.printf("Queue capacity : %d%n", QUEUE_CAPACITY);
        System.out.printf("Producers      : 2%n");
        System.out.printf("Consumers      : 3%n%n");

        int producerCount = 2;
        int consumerCount = 3;

        // ── Start Producers ──────────────────────────────────────────────
        Thread[] producers = new Thread[producerCount];
        for (int p = 0; p < producerCount; p++) {
            final int producerId = p + 1;
            producers[p] = new Thread(() -> {
                try {
                    String[] items = {"Laptop", "Phone", "Tablet", "Monitor", "Keyboard"};
                    for (int i = 0; i < items.length; i++) {
                        Order order = new Order(producerId * 100 + i, items[i]);
                        System.out.printf("  [Producer-%d]  → putting %s  (queue size: %d)%n",
                                producerId, order, orderQueue.size());

                        orderQueue.put(order);  // BLOCKS if queue is full

                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
                    }
                    System.out.printf("  [Producer-%d]  ✓ finished producing%n", producerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + producerId);
            producers[p].start();
        }

        // ── Start Consumers ──────────────────────────────────────────────
        Thread[] consumers = new Thread[consumerCount];
        for (int c = 0; c < consumerCount; c++) {
            final int consumerId = c + 1;
            consumers[c] = new Thread(() -> {
                try {
                    while (true) {
                        Order order = orderQueue.take();  // BLOCKS if queue is empty

                        if (order == POISON_PILL) {
                            System.out.printf("  [Consumer-%d]  ☠ received shutdown signal%n", consumerId);
                            break;
                        }

                        System.out.printf("  [Consumer-%d]  ← processing %s …%n", consumerId, order);
                        // simulate processing time
                        Thread.sleep(ThreadLocalRandom.current().nextInt(300, 700));
                        System.out.printf("  [Consumer-%d]  ✓ completed  %s%n", consumerId, order);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + consumerId);
            consumers[c].start();
        }

        // ── Wait for producers to finish ─────────────────────────────────
        for (Thread p : producers) p.join();
        System.out.println("\n  All producers finished. Sending poison pills to consumers …\n");

        // ── Graceful shutdown: send one poison pill per consumer ──────────
        for (int c = 0; c < consumerCount; c++) {
            orderQueue.put(POISON_PILL);
        }

        for (Thread c : consumers) c.join();

        System.out.println("\n✔ All orders processed. Queue is empty: " + orderQueue.isEmpty());
    }
}

