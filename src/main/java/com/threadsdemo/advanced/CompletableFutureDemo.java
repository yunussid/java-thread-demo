package com.threadsdemo.advanced;

import java.util.concurrent.*;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * COMPLETABLE FUTURE DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * CompletableFuture provides non-blocking async programming with:
 * - Chaining operations (thenApply, thenCompose)
 * - Combining futures (thenCombine, allOf, anyOf)
 * - Exception handling (exceptionally, handle)
 * - Custom executors
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class CompletableFutureDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture Demo ===\n");

        basicDemo();
        chainingDemo();
        combiningDemo();
        allOfAnyOfDemo();
        exceptionHandlingDemo();
        realWorldExampleDemo();
    }

    /**
     * Basic CompletableFuture creation
     */
    static void basicDemo() throws Exception {
        System.out.println("--- Basic Demo ---");

        // supplyAsync - Returns a value
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("supplyAsync running on: " + Thread.currentThread().getName());
            return "Hello from supplyAsync";
        });
        System.out.println("Result: " + future1.get());

        // runAsync - No return value
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            System.out.println("runAsync running on: " + Thread.currentThread().getName());
        });
        future2.get();

        // completedFuture - Already completed
        CompletableFuture<String> future3 = CompletableFuture.completedFuture("Already done!");
        System.out.println("Completed future: " + future3.get());

        System.out.println();
    }

    /**
     * Chaining operations
     */
    static void chainingDemo() throws Exception {
        System.out.println("--- Chaining Demo ---");

        // thenApply - Transform result (like map)
        CompletableFuture<String> future1 = CompletableFuture
            .supplyAsync(() -> "hello")
            .thenApply(s -> s.toUpperCase())
            .thenApply(s -> s + " WORLD");
        System.out.println("thenApply: " + future1.get());

        // thenCompose - Chain dependent futures (like flatMap)
        CompletableFuture<String> future2 = CompletableFuture
            .supplyAsync(() -> "user:123")
            .thenCompose(userId -> getUserDetails(userId));
        System.out.println("thenCompose: " + future2.get());

        // thenAccept - Consume result
        CompletableFuture.supplyAsync(() -> "Data to process")
            .thenAccept(data -> System.out.println("thenAccept consumed: " + data))
            .get();

        // thenRun - Run action after completion
        CompletableFuture.supplyAsync(() -> "ignored")
            .thenRun(() -> System.out.println("thenRun: Task completed!"))
            .get();

        System.out.println();
    }

    static CompletableFuture<String> getUserDetails(String userId) {
        return CompletableFuture.supplyAsync(() -> "Details for " + userId);
    }

    /**
     * Combining futures
     */
    static void combiningDemo() throws Exception {
        System.out.println("--- Combining Demo ---");

        // thenCombine - Combine two independent futures
        CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return "John";
        });

        CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> {
            sleep(100);
            return 30;
        });

        CompletableFuture<String> combined = nameFuture.thenCombine(ageFuture,
            (name, age) -> name + " is " + age + " years old");

        System.out.println("thenCombine: " + combined.get());

        System.out.println();
    }

    /**
     * allOf and anyOf
     */
    static void allOfAnyOfDemo() throws Exception {
        System.out.println("--- allOf / anyOf Demo ---");

        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
            sleep(100); return "Task1";
        });
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
            sleep(200); return "Task2";
        });
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> {
            sleep(150); return "Task3";
        });

        // allOf - Wait for ALL to complete
        long startAll = System.currentTimeMillis();
        CompletableFuture.allOf(f1, f2, f3).join();
        System.out.println("allOf completed in: " + (System.currentTimeMillis() - startAll) + "ms");
        System.out.println("Results: " + f1.get() + ", " + f2.get() + ", " + f3.get());

        // Reset futures
        CompletableFuture<String> fast = CompletableFuture.supplyAsync(() -> {
            sleep(50); return "Fast";
        });
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            sleep(500); return "Slow";
        });

        // anyOf - Complete when ANY completes
        long startAny = System.currentTimeMillis();
        Object firstResult = CompletableFuture.anyOf(fast, slow).get();
        System.out.println("anyOf first result: " + firstResult +
            " in " + (System.currentTimeMillis() - startAny) + "ms");

        System.out.println();
    }

    /**
     * Exception handling
     */
    static void exceptionHandlingDemo() throws Exception {
        System.out.println("--- Exception Handling Demo ---");

        // exceptionally - Handle exception with fallback
        CompletableFuture<String> future1 = CompletableFuture
            .supplyAsync(() -> {
                if (true) throw new RuntimeException("Oops!");
                return "Success";
            })
            .exceptionally(ex -> {
                System.out.println("exceptionally caught: " + ex.getMessage());
                return "Fallback Value";
            });
        System.out.println("Result: " + future1.get());

        // handle - Handle both success and failure
        CompletableFuture<String> future2 = CompletableFuture
            .supplyAsync(() -> "Success!")
            .handle((result, ex) -> {
                if (ex != null) {
                    return "Error: " + ex.getMessage();
                }
                return "Handle success: " + result;
            });
        System.out.println("handle: " + future2.get());

        // whenComplete - Side effect (doesn't change result)
        CompletableFuture.supplyAsync(() -> "Result")
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    System.out.println("whenComplete error: " + ex);
                } else {
                    System.out.println("whenComplete success: " + result);
                }
            })
            .get();

        System.out.println();
    }

    /**
     * Real-world example: Parallel API calls
     */
    static void realWorldExampleDemo() throws Exception {
        System.out.println("--- Real-World Example: Parallel API Calls ---");

        long start = System.currentTimeMillis();

        // Simulate parallel API calls
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching user profile...");
            sleep(200);
            return "User: John Doe";
        });

        CompletableFuture<String> ordersFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching user orders...");
            sleep(300);
            return "Orders: [Order1, Order2, Order3]";
        });

        CompletableFuture<String> recsFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("Fetching recommendations...");
            sleep(250);
            return "Recommendations: [Prod1, Prod2]";
        });

        // Combine all results
        CompletableFuture<String> dashboard = CompletableFuture
            .allOf(userFuture, ordersFuture, recsFuture)
            .thenApply(v -> {
                String user = userFuture.join();
                String orders = ordersFuture.join();
                String recs = recsFuture.join();
                return String.format("Dashboard:\n  %s\n  %s\n  %s", user, orders, recs);
            });

        System.out.println(dashboard.get());
        System.out.println("Total time: " + (System.currentTimeMillis() - start) + "ms");
        System.out.println("(Sequential would take: 750ms, Parallel: ~300ms)");

        System.out.println();
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
