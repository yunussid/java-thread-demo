package com.threadsdemo.advanced;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXECUTOR SERVICE & THREAD POOLS DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ExecutorService manages a pool of reusable threads:
 * - FixedThreadPool: Fixed number of threads
 * - CachedThreadPool: Creates threads as needed
 * - SingleThreadExecutor: Single worker thread
 * - ScheduledThreadPool: For delayed/periodic tasks
 * - WorkStealingPool: Fork-join based (Java 8+)
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class ExecutorServiceDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== ExecutorService Demo ===\n");

        fixedThreadPoolDemo();
        cachedThreadPoolDemo();
        singleThreadExecutorDemo();
        scheduledThreadPoolDemo();
        futureDemo();
        invokeAllDemo();
        customThreadPoolDemo();
    }

    /**
     * Fixed Thread Pool - Best for known workload
     */
    static void fixedThreadPoolDemo() throws InterruptedException {
        System.out.println("--- Fixed Thread Pool Demo ---");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Submit 6 tasks to 3 threads
        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " running on " +
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Proper shutdown
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("All tasks completed\n");
    }

    /**
     * Cached Thread Pool - Best for many short tasks
     */
    static void cachedThreadPoolDemo() throws InterruptedException {
        System.out.println("--- Cached Thread Pool Demo ---");

        ExecutorService executor = Executors.newCachedThreadPool();

        // Submit 5 tasks - may create up to 5 threads
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " on " +
                    Thread.currentThread().getName());
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Threads are cached for 60 seconds for reuse\n");
    }

    /**
     * Single Thread Executor - Guaranteed sequential execution
     */
    static void singleThreadExecutorDemo() throws InterruptedException {
        System.out.println("--- Single Thread Executor Demo ---");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " on " +
                    Thread.currentThread().getName() + " (always same thread)");
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println();
    }

    /**
     * Scheduled Thread Pool - Delayed and periodic tasks
     */
    static void scheduledThreadPoolDemo() throws InterruptedException {
        System.out.println("--- Scheduled Thread Pool Demo ---");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // Schedule with delay
        System.out.println("Scheduling task with 1 second delay...");
        scheduler.schedule(() -> {
            System.out.println("Delayed task executed!");
        }, 1, TimeUnit.SECONDS);

        // Schedule periodic task
        System.out.println("Scheduling periodic task every 500ms...");
        ScheduledFuture<?> periodicFuture = scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Periodic task at " + System.currentTimeMillis() % 10000);
        }, 0, 500, TimeUnit.MILLISECONDS);

        // Let it run for 2 seconds
        Thread.sleep(2000);
        periodicFuture.cancel(false);

        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println();
    }

    /**
     * Future - Get result from async task
     */
    static void futureDemo() throws Exception {
        System.out.println("--- Future Demo ---");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Submit Callable that returns a value
        Future<Integer> future = executor.submit(() -> {
            System.out.println("Computing...");
            Thread.sleep(1000);
            return 42;
        });

        System.out.println("Task submitted, doing other work...");
        System.out.println("Is done? " + future.isDone());

        // Block and get result
        Integer result = future.get();  // Blocks until complete
        System.out.println("Result: " + result);
        System.out.println("Is done? " + future.isDone());

        executor.shutdown();
        System.out.println();
    }

    /**
     * invokeAll - Execute multiple tasks and wait for all
     */
    static void invokeAllDemo() throws Exception {
        System.out.println("--- invokeAll Demo ---");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(taskId * 100);
                return "Result-" + taskId;
            });
        }

        // Execute all and wait
        List<Future<String>> futures = executor.invokeAll(tasks);

        System.out.println("All tasks completed:");
        for (Future<String> f : futures) {
            System.out.println("  " + f.get());
        }

        executor.shutdown();
        System.out.println();
    }

    /**
     * Custom Thread Pool with ThreadPoolExecutor
     */
    static void customThreadPoolDemo() throws InterruptedException {
        System.out.println("--- Custom Thread Pool Demo ---");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                      // corePoolSize
            4,                      // maximumPoolSize
            60L, TimeUnit.SECONDS,  // keepAliveTime
            new LinkedBlockingQueue<>(10),  // workQueue (bounded)
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CustomThread-" + count++);
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()  // Rejection policy
        );

        System.out.println("Core pool size: " + executor.getCorePoolSize());
        System.out.println("Max pool size: " + executor.getMaximumPoolSize());

        // Submit tasks
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " on " +
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(100);
        System.out.println("Active threads: " + executor.getActiveCount());
        System.out.println("Pool size: " + executor.getPoolSize());
        System.out.println("Queue size: " + executor.getQueue().size());

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println();
    }
}
