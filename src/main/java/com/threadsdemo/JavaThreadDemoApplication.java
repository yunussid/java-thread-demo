package com.threadsdemo;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *                      JAVA THREADING DEMO - MAIN APPLICATION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * A comprehensive guide to Java threading concepts:
 * 
 * 1. BASICS (basics/)
 *    - Thread creation (Thread class, Runnable, Callable)
 *    - Thread lifecycle and states
 *    - Thread methods (sleep, join, interrupt)
 *    - Daemon threads
 * 
 * 2. SYNCHRONIZATION (synchronization/)
 *    - synchronized keyword
 *    - ReentrantLock
 *    - ReadWriteLock
 *    - volatile keyword
 *    - Atomic classes
 * 
 * 3. EXECUTORS (executors/)
 *    - ThreadPoolExecutor
 *    - FixedThreadPool, CachedThreadPool, ScheduledThreadPool
 *    - Custom thread pools
 *    - Fork/Join framework
 * 
 * 4. COMPLETABLE FUTURE (completablefuture/)
 *    - Creating async tasks
 *    - Chaining operations (thenApply, thenCompose)
 *    - Combining futures (allOf, anyOf)
 *    - Exception handling
 * 
 * 5. CONCURRENT COLLECTIONS (concurrent/)
 *    - ConcurrentHashMap
 *    - CopyOnWriteArrayList
 *    - BlockingQueue
 *    - ConcurrentLinkedQueue
 * 
 * 6. CLASSIC PROBLEMS (problems/)
 *    - Producer-Consumer
 *    - Dining Philosophers
 *    - Reader-Writer
 *    - Deadlock demonstration
 * 
 * 7. VIRTUAL THREADS (virtualthreads/) - Java 21
 *    - Creating virtual threads
 *    - Scalability benefits
 *    - Migration from platform threads
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 */
public class JavaThreadDemoApplication {

    public static void main(String[] args) {
        System.out.println("""
            ╔═══════════════════════════════════════════════════════════════════════╗
            ║                                                                       ║
            ║              JAVA THREADING DEMO APPLICATION                          ║
            ║                                                                       ║
            ║     Explore each package for different threading concepts             ║
            ║                                                                       ║
            ╚═══════════════════════════════════════════════════════════════════════╝
            
            Available Demos:
            ─────────────────
            1. basics/           - Thread creation and lifecycle
            2. synchronization/  - Locks, synchronized, volatile
            3. executors/        - Thread pools and executors
            4. completablefuture/- Async programming
            5. concurrent/       - Thread-safe collections
            6. problems/         - Classic threading problems
            7. virtualthreads/   - Java 21 virtual threads
            
            Run any class with a main() method to see the demo!
            """);
    }
}

