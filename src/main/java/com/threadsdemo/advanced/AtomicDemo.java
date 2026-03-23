package com.threadsdemo.advanced;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ATOMIC CLASSES DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Atomic classes provide lock-free, thread-safe operations using CAS
 * (Compare-And-Swap) at the hardware level.
 *
 * Available classes:
 * - AtomicInteger, AtomicLong, AtomicBoolean
 * - AtomicReference<V>
 * - AtomicIntegerArray, AtomicLongArray
 * - LongAdder, DoubleAdder (high contention scenarios)
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class AtomicDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Atomic Classes Demo ===\n");

        atomicIntegerDemo();
        atomicReferenceDemo();
        compareAndSetDemo();
        longAdderDemo();
    }

    /**
     * AtomicInteger - Thread-safe integer operations
     */
    static void atomicIntegerDemo() throws InterruptedException {
        System.out.println("--- AtomicInteger Demo ---");

        AtomicInteger counter = new AtomicInteger(0);

        // Multiple threads incrementing
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.incrementAndGet();  // Atomic increment
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        System.out.println("Counter: " + counter.get() + " (expected: 10000)");

        // Other operations
        System.out.println("incrementAndGet: " + counter.incrementAndGet());
        System.out.println("getAndIncrement: " + counter.getAndIncrement());
        System.out.println("addAndGet(5): " + counter.addAndGet(5));
        System.out.println("getAndAdd(3): " + counter.getAndAdd(3));
        System.out.println("Final: " + counter.get());

        System.out.println();
    }

    /**
     * AtomicReference - Thread-safe object references
     */
    static void atomicReferenceDemo() throws InterruptedException {
        System.out.println("--- AtomicReference Demo ---");

        record User(String name, int age) {}

        AtomicReference<User> userRef = new AtomicReference<>(new User("Alice", 25));

        System.out.println("Initial: " + userRef.get());

        // Update atomically
        Thread t1 = new Thread(() -> {
            User current = userRef.get();
            User updated = new User(current.name(), current.age() + 1);
            if (userRef.compareAndSet(current, updated)) {
                System.out.println("Thread 1: Updated age to " + updated.age());
            } else {
                System.out.println("Thread 1: CAS failed, someone else updated");
            }
        });

        Thread t2 = new Thread(() -> {
            User current = userRef.get();
            User updated = new User("Bob", current.age());
            if (userRef.compareAndSet(current, updated)) {
                System.out.println("Thread 2: Changed name to " + updated.name());
            } else {
                System.out.println("Thread 2: CAS failed, someone else updated");
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Final: " + userRef.get());
        System.out.println();
    }

    /**
     * Compare-And-Set (CAS) pattern
     */
    static void compareAndSetDemo() {
        System.out.println("--- Compare-And-Set Demo ---");

        AtomicInteger value = new AtomicInteger(100);

        // CAS succeeds when expected value matches
        boolean success1 = value.compareAndSet(100, 200);
        System.out.println("CAS(100 -> 200): " + success1 + ", value: " + value.get());

        // CAS fails when expected value doesn't match
        boolean success2 = value.compareAndSet(100, 300);
        System.out.println("CAS(100 -> 300): " + success2 + ", value: " + value.get());

        // Update using updateAndGet (functional style)
        int result = value.updateAndGet(v -> v * 2);
        System.out.println("updateAndGet(v * 2): " + result);

        // Accumulate using accumulateAndGet
        int accumulated = value.accumulateAndGet(50, Integer::sum);
        System.out.println("accumulateAndGet(50, sum): " + accumulated);

        System.out.println();
    }

    /**
     * LongAdder - High-performance counter for high contention
     */
    static void longAdderDemo() throws InterruptedException {
        System.out.println("--- LongAdder vs AtomicLong Performance ---");

        int threadCount = 10;
        int incrementsPerThread = 1_000_000;

        // Test AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long atomicStart = System.currentTimeMillis();

        Thread[] atomicThreads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            atomicThreads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    atomicLong.incrementAndGet();
                }
            });
            atomicThreads[i].start();
        }
        for (Thread t : atomicThreads) t.join();

        long atomicTime = System.currentTimeMillis() - atomicStart;

        // Test LongAdder
        LongAdder longAdder = new LongAdder();
        long adderStart = System.currentTimeMillis();

        Thread[] adderThreads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            adderThreads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    longAdder.increment();
                }
            });
            adderThreads[i].start();
        }
        for (Thread t : adderThreads) t.join();

        long adderTime = System.currentTimeMillis() - adderStart;

        System.out.println("AtomicLong: " + atomicLong.get() + " in " + atomicTime + "ms");
        System.out.println("LongAdder:  " + longAdder.sum() + " in " + adderTime + "ms");
        System.out.println("LongAdder is " + (atomicTime / (double) adderTime) + "x faster!");
        System.out.println();
    }
}
