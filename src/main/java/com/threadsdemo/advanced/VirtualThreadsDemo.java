package com.threadsdemo.advanced;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * VIRTUAL THREADS DEMO (Java 21+)
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Virtual Threads are lightweight threads that:
 * - Are managed by JVM (not OS)
 * - Use very little memory (~KB vs ~MB for platform threads)
 * - Enable massive concurrency (millions vs thousands)
 * - Perfect for I/O-bound operations
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class VirtualThreadsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Virtual Threads Demo (Java 21+) ===\n");

        creationDemo();
        executorDemo();
        scalabilityDemo();
        ioTaskDemo();
    }

    /**
     * Different ways to create virtual threads
     */
    static void creationDemo() throws InterruptedException {
        System.out.println("--- Virtual Thread Creation ---");

        // Method 1: Thread.startVirtualThread()
        Thread vt1 = Thread.startVirtualThread(() -> {
            System.out.println("VT1 running on: " + Thread.currentThread());
            System.out.println("VT1 is virtual: " + Thread.currentThread().isVirtual());
        });
        vt1.join();

        // Method 2: Thread.ofVirtual().start()
        Thread vt2 = Thread.ofVirtual()
            .name("my-virtual-thread")
            .start(() -> {
                System.out.println("VT2 name: " + Thread.currentThread().getName());
            });
        vt2.join();

        // Method 3: Thread.ofVirtual().factory()
        ThreadFactory factory = Thread.ofVirtual().name("worker-", 0).factory();
        Thread vt3 = factory.newThread(() -> {
            System.out.println("VT3 (factory): " + Thread.currentThread().getName());
        });
        vt3.start();
        vt3.join();

        // Compare with platform thread
        Thread pt = Thread.ofPlatform()
            .name("platform-thread")
            .start(() -> {
                System.out.println("Platform thread: " + Thread.currentThread().getName());
                System.out.println("Platform is virtual: " + Thread.currentThread().isVirtual());
            });
        pt.join();

        System.out.println();
    }

    /**
     * Virtual thread executor
     */
    static void executorDemo() throws Exception {
        System.out.println("--- Virtual Thread Executor ---");

        // newVirtualThreadPerTaskExecutor - creates new VT for each task
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Future<String> f1 = executor.submit(() -> {
                Thread.sleep(100);
                return "Task 1 on " + Thread.currentThread();
            });

            Future<String> f2 = executor.submit(() -> {
                Thread.sleep(100);
                return "Task 2 on " + Thread.currentThread();
            });

            System.out.println(f1.get());
            System.out.println(f2.get());
        }

        System.out.println();
    }

    /**
     * Scalability comparison: Virtual vs Platform threads
     */
    static void scalabilityDemo() throws Exception {
        System.out.println("--- Scalability Comparison ---");

        int taskCount = 10_000;

        // Virtual Threads
        System.out.println("Starting " + taskCount + " virtual threads...");
        Instant start1 = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, taskCount).forEach(i -> {
                executor.submit(() -> {
                    try {
                        Thread.sleep(100);  // Simulate I/O
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return i;
                });
            });
        }

        Duration duration1 = Duration.between(start1, Instant.now());
        System.out.println("Virtual threads: " + duration1.toMillis() + "ms");

        // Platform Threads (limited pool)
        System.out.println("Starting " + taskCount + " tasks on platform thread pool (100 threads)...");
        Instant start2 = Instant.now();

        try (var executor = Executors.newFixedThreadPool(100)) {
            IntStream.range(0, taskCount).forEach(i -> {
                executor.submit(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return i;
                });
            });
        }

        Duration duration2 = Duration.between(start2, Instant.now());
        System.out.println("Platform threads (pool=100): " + duration2.toMillis() + "ms");

        System.out.println("Virtual threads are ~" +
            String.format("%.1f", (double) duration2.toMillis() / duration1.toMillis()) +
            "x faster!");

        System.out.println();
    }

    /**
     * I/O-bound tasks - where virtual threads shine
     */
    static void ioTaskDemo() throws Exception {
        System.out.println("--- I/O-Bound Tasks Demo ---");

        // Simulating concurrent HTTP requests
        int requestCount = 100;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            Instant start = Instant.now();

            var futures = IntStream.range(0, requestCount)
                .mapToObj(i -> executor.submit(() -> {
                    // Simulate HTTP request (I/O bound)
                    Thread.sleep(500);
                    return "Response-" + i;
                }))
                .toList();

            // Wait for all to complete
            for (var future : futures) {
                future.get();
            }

            Duration duration = Duration.between(start, Instant.now());
            System.out.println("Completed " + requestCount + " 'HTTP requests' in " +
                duration.toMillis() + "ms");
            System.out.println("(Each request takes 500ms, but they run concurrently!)");
        }

        System.out.println();
    }
}

/*
═══════════════════════════════════════════════════════════════════════════════
VIRTUAL THREADS BEST PRACTICES:

✅ DO:
- Use for I/O-bound operations (HTTP, DB, file I/O)
- Use try-with-resources with executor
- Use newVirtualThreadPerTaskExecutor()

❌ DON'T:
- Use for CPU-bound operations
- Pool virtual threads (they're cheap, just create new ones)
- Use synchronized blocks (pins carrier thread) - use ReentrantLock instead

MIGRATION:
Just change:
  Executors.newFixedThreadPool(N)
To:
  Executors.newVirtualThreadPerTaskExecutor()

═══════════════════════════════════════════════════════════════════════════════
*/
