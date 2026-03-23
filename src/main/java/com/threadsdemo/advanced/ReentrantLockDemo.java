package com.threadsdemo.advanced;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * REENTRANT LOCK DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ReentrantLock provides more flexibility than synchronized:
 * - tryLock() - Non-blocking lock attempt
 * - tryLock(timeout) - Timed lock attempt
 * - lockInterruptibly() - Can be interrupted while waiting
 * - Fair lock option - FIFO ordering
 * - Multiple Condition variables
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class ReentrantLockDemo {

    private final ReentrantLock lock = new ReentrantLock();
    private final ReentrantLock fairLock = new ReentrantLock(true);  // Fair lock
    private int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        ReentrantLockDemo demo = new ReentrantLockDemo();

        System.out.println("=== ReentrantLock Demo ===\n");

        demo.basicLockDemo();
        demo.tryLockDemo();
        demo.timedLockDemo();
        demo.conditionDemo();
    }

    /**
     * Basic lock/unlock usage
     */
    public void basicLockDemo() throws InterruptedException {
        System.out.println("--- Basic Lock Demo ---");

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                increment();
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                increment();
            }
        }, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Counter value: " + counter + " (expected: 2000)");
        System.out.println();
    }

    public void increment() {
        lock.lock();  // Acquire lock
        try {
            counter++;
        } finally {
            lock.unlock();  // ALWAYS release in finally!
        }
    }

    /**
     * tryLock - Non-blocking lock attempt
     */
    public void tryLockDemo() {
        System.out.println("--- Try Lock Demo ---");

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("Thread holding lock for 2 seconds...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "Holder");

        Thread tryLocker = new Thread(() -> {
            try {
                Thread.sleep(500);  // Let holder acquire first
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (lock.tryLock()) {
                try {
                    System.out.println("tryLock succeeded!");
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println("tryLock failed - lock not available");
            }
        }, "TryLocker");

        holder.start();
        tryLocker.start();

        try {
            holder.join();
            tryLocker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
    }

    /**
     * Timed tryLock - Wait up to timeout
     */
    public void timedLockDemo() throws InterruptedException {
        System.out.println("--- Timed Lock Demo ---");

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("Holder: Got lock, holding for 3 seconds...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                System.out.println("Holder: Released lock");
            }
        });

        Thread waiter = new Thread(() -> {
            try {
                Thread.sleep(500);
                System.out.println("Waiter: Trying to acquire lock with 2 second timeout...");

                if (lock.tryLock(2, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("Waiter: Got the lock!");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("Waiter: Timeout! Could not get lock in 2 seconds.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        holder.start();
        waiter.start();
        holder.join();
        waiter.join();

        System.out.println();
    }

    /**
     * Condition variables - replacement for wait/notify
     */
    public void conditionDemo() throws InterruptedException {
        System.out.println("--- Condition Variable Demo ---");

        BoundedBuffer buffer = new BoundedBuffer(3);

        // Producer
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.put(i);
                    System.out.println("Produced: " + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    int item = buffer.take();
                    System.out.println("Consumed: " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        System.out.println();
    }

    /**
     * Bounded Buffer using Condition variables
     */
    static class BoundedBuffer {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();
        private final Condition notEmpty = lock.newCondition();

        private final int[] items;
        private int count = 0;
        private int putIndex = 0;
        private int takeIndex = 0;

        public BoundedBuffer(int capacity) {
            items = new int[capacity];
        }

        public void put(int item) throws InterruptedException {
            lock.lock();
            try {
                while (count == items.length) {
                    System.out.println("Buffer full, producer waiting...");
                    notFull.await();
                }
                items[putIndex] = item;
                putIndex = (putIndex + 1) % items.length;
                count++;
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        public int take() throws InterruptedException {
            lock.lock();
            try {
                while (count == 0) {
                    System.out.println("Buffer empty, consumer waiting...");
                    notEmpty.await();
                }
                int item = items[takeIndex];
                takeIndex = (takeIndex + 1) % items.length;
                count--;
                notFull.signal();
                return item;
            } finally {
                lock.unlock();
            }
        }
    }
}
