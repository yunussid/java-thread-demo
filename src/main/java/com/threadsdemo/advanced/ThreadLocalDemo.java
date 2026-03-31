package com.threadsdemo.advanced;

import java.util.concurrent.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * THREAD-LOCAL DEMO — Context Isolation
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ThreadLocal provides variables that are local to a specific thread.
 * Each thread has its own, independently initialized copy.
 *
 * Use Case — Passing "Context" Through a Deep Call Stack:
 *   In Spring, SecurityContextHolder uses ThreadLocal to store the current
 *   user's authentication details. This allows any service in that thread's
 *   execution path to call SecurityContextHolder.getContext() and retrieve
 *   the user ID — without you having to pass it as a method parameter
 *   through every layer (Controller → Service → Repository → Audit …).
 *
 * ⚠ WARNING — ThreadLocal + Thread Pools = Data Leak ⚠
 *   Thread pools (Tomcat, @Async, Executors) REUSE threads. If you forget
 *   to call ThreadLocal.remove(), data from one request can "leak" into
 *   the next request handled by the same pooled thread.
 *   ALWAYS call .remove() in a finally block!
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class ThreadLocalDemo {

    // ── Simulated "RequestContext" stored per-thread ──────────────────────
    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    record RequestContext(String requestId, String userId, long startTimeMs) {
        @Override
        public String toString() {
            return "RequestContext{requestId='%s', userId='%s'}".formatted(requestId, userId);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) throws Exception {
        System.out.println("=== ThreadLocal: Context Isolation Demo ===\n");

        basicIsolationDemo();
        System.out.println("═".repeat(70) + "\n");

        dataLeakDemo();
        System.out.println("═".repeat(70) + "\n");

        safeUsageDemo();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 1. Basic Isolation — each thread sees its own copy
    // ──────────────────────────────────────────────────────────────────────
    static void basicIsolationDemo() throws InterruptedException {
        System.out.println("--- Basic Isolation ---");
        System.out.println("Two threads set different values; they never interfere.\n");

        Thread alice = new Thread(() -> {
            CONTEXT.set(new RequestContext("REQ-001", "alice", System.currentTimeMillis()));
            simulateDeepCallStack("alice");
            CONTEXT.remove();
        }, "alice-thread");

        Thread bob = new Thread(() -> {
            CONTEXT.set(new RequestContext("REQ-002", "bob", System.currentTimeMillis()));
            simulateDeepCallStack("bob");
            CONTEXT.remove();
        }, "bob-thread");

        alice.start();
        bob.start();
        alice.join();
        bob.join();

        System.out.println("\n✔ Each thread accessed only its own context.\n");
    }

    /** Simulates Controller → Service → Repository, all reading from ThreadLocal */
    private static void simulateDeepCallStack(String label) {
        System.out.printf("  [%s] Controller — context: %s%n", label, CONTEXT.get());
        serviceLayer(label);
    }

    private static void serviceLayer(String label) {
        System.out.printf("  [%s] Service    — context: %s%n", label, CONTEXT.get());
        repositoryLayer(label);
    }

    private static void repositoryLayer(String label) {
        System.out.printf("  [%s] Repository — context: %s%n", label, CONTEXT.get());
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2. DATA LEAK — what happens when you forget .remove()
    // ──────────────────────────────────────────────────────────────────────

    /**
     * A single-thread pool reuses the SAME thread for every task.
     * If we forget .remove(), the context from Request-1 leaks into Request-2.
     */
    static void dataLeakDemo() throws Exception {
        System.out.println("--- ⚠ DATA LEAK — Forgetting .remove() ---");
        System.out.println("Using a single-thread pool (simulates Tomcat reusing a thread).\n");

        // ThreadLocal without cleanup
        ThreadLocal<String> leakyLocal = new ThreadLocal<>();

        ExecutorService pool = Executors.newSingleThreadExecutor();

        // Request 1 — sets "User-Alice" but NEVER calls remove()
        pool.submit(() -> {
            leakyLocal.set("User-Alice");
            System.out.println("  Request-1  set context → " + leakyLocal.get());
            // ⚠ No leakyLocal.remove() here!
        }).get(); // wait for completion

        // Request 2 — does NOT set any context, but reads it
        pool.submit(() -> {
            String leaked = leakyLocal.get();
            System.out.println("  Request-2  read context → " + leaked);
            if (leaked != null) {
                System.out.println("  ⚠ BUG! Request-2 sees Alice's data from Request-1!");
            }
        }).get();

        pool.shutdown();
        System.out.println();
    }

    // ──────────────────────────────────────────────────────────────────────
    // 3. SAFE USAGE — always .remove() in a finally block
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Same scenario, but with proper cleanup. No data leaks.
     */
    static void safeUsageDemo() throws Exception {
        System.out.println("--- ✔ SAFE USAGE — Always call .remove() ---\n");

        ThreadLocal<String> safeLocal = new ThreadLocal<>();

        ExecutorService pool = Executors.newSingleThreadExecutor();

        // Request 1 — sets context AND removes it in finally
        pool.submit(() -> {
            try {
                safeLocal.set("User-Alice");
                System.out.println("  Request-1  set context → " + safeLocal.get());
                // … do work …
            } finally {
                safeLocal.remove();  // ✔ ALWAYS clean up
                System.out.println("  Request-1  removed context in finally block");
            }
        }).get();

        // Request 2 — context is clean
        pool.submit(() -> {
            String value = safeLocal.get();
            System.out.println("  Request-2  read context → " + value);
            if (value == null) {
                System.out.println("  ✔ Clean! No leaked data.");
            }
        }).get();

        pool.shutdown();
        System.out.println("\n✔ ThreadLocal demo finished.\n");
    }
}

