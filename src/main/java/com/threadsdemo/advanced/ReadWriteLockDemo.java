package com.threadsdemo.advanced;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * READ-WRITE LOCK DEMO
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ReadWriteLock allows:
 * - Multiple readers simultaneously (read lock is shared)
 * - Only ONE writer at a time (write lock is exclusive)
 * - Writers block readers and other writers
 *
 * Perfect for read-heavy scenarios like caches!
 *
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class ReadWriteLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ReadWriteLock Demo ===\n");

        Cache cache = new Cache();

        // Pre-populate cache
        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");
        cache.put("user:3", "Charlie");

        // Multiple readers
        Thread reader1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Reader1: " + cache.get("user:1"));
                sleep(100);
            }
        }, "Reader-1");

        Thread reader2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Reader2: " + cache.get("user:2"));
                sleep(100);
            }
        }, "Reader-2");

        Thread reader3 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Reader3: " + cache.get("user:3"));
                sleep(100);
            }
        }, "Reader-3");

        // Single writer
        Thread writer = new Thread(() -> {
            sleep(200);  // Let readers start first
            System.out.println("Writer: Updating user:1...");
            cache.put("user:1", "Alice Updated");
            System.out.println("Writer: Update complete!");
        }, "Writer");

        long start = System.currentTimeMillis();

        reader1.start();
        reader2.start();
        reader3.start();
        writer.start();

        reader1.join();
        reader2.join();
        reader3.join();
        writer.join();

        long duration = System.currentTimeMillis() - start;
        System.out.println("\nCompleted in " + duration + "ms");
        System.out.println("Final value: " + cache.get("user:1"));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Thread-safe cache using ReadWriteLock
 */
class Cache {

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, Object> cache = new HashMap<>();

    /**
     * Multiple threads can read simultaneously
     */
    public Object get(String key) {
        rwLock.readLock().lock();
        try {
            // Simulate some read operation time
            Thread.sleep(50);
            return cache.get(key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Only one thread can write at a time
     * Writers block all readers and other writers
     */
    public void put(String key, Object value) {
        rwLock.writeLock().lock();
        try {
            // Simulate some write operation time
            Thread.sleep(200);
            cache.put(key, value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Remove from cache
     */
    public void remove(String key) {
        rwLock.writeLock().lock();
        try {
            cache.remove(key);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Get all entries (read operation)
     */
    public Map<String, Object> getAll() {
        rwLock.readLock().lock();
        try {
            return new HashMap<>(cache);  // Return a copy
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Clear cache (write operation)
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
