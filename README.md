# Java Threading & Concurrency -- The Complete Guide

A hands-on, runnable reference for every major Java concurrency concept. Each class has a `main()` method -- run it and see the output.

> **Never written Java?** That is fine. This README explains every concept from scratch before showing the code.

---

## Before You Start -- What is a Thread?

### The Problem: One Thing at a Time

By default, a Java program runs on a **single thread** -- it does one thing at a time, top to bottom:

```
fetch user from database     (500ms -- program is STUCK waiting)
fetch orders from database   (300ms -- can not start until user fetch is done)
fetch recommendations        (200ms -- waits for orders to finish)

Total: 1000ms
```

### The Solution: Threads

A **thread** is an independent path of execution inside your program. With multiple threads, you can do multiple things at the same time:

```
Thread 1: fetch user from database       (500ms)
Thread 2: fetch orders from database     (300ms)  -- starts at the SAME TIME
Thread 3: fetch recommendations          (200ms)  -- starts at the SAME TIME

Total: 500ms (the slowest one)
```

### The Danger: Shared Data

When two threads access the **same variable** at the same time, things break:

```
Thread A reads counter = 10
Thread B reads counter = 10       (at the same time!)
Thread A writes counter = 11
Thread B writes counter = 11      (should be 12, but B still thinks it was 10)

Result: counter = 11 (WRONG -- should be 12)
```

This is called a **race condition**. The entire `java.util.concurrent` package exists to solve this problem. That is what this project teaches.

### Key Vocabulary

| Term | Meaning |
|------|---------|
| **Thread** | An independent path of execution inside your program |
| **Main Thread** | The thread that runs your `main()` method. Every Java program starts with one |
| **Multithreading** | Running multiple threads at the same time |
| **Concurrency** | Designing your program to handle multiple tasks that may overlap in time |
| **Parallelism** | Actually running tasks at the exact same instant (requires multiple CPU cores) |
| **Race Condition** | A bug where the result depends on which thread runs first |
| **Deadlock** | Two threads waiting for each other forever -- both stuck |
| **Thread Safety** | Code that works correctly even when called from multiple threads simultaneously |
| **Synchronization** | Controlling the order in which threads access shared data |
| **Critical Section** | A block of code that must not be executed by two threads at the same time |
| **Lock** | A mechanism to ensure only one thread enters a critical section |
| **Atomic** | An operation that completes in one step -- no other thread can see it half-done |

---

## Project Structure -- Read the Code in This Order

```
java-thread-demo/
  |
  +-- pom.xml                                        (Maven config, Java 21+)
  |
  +-- src/main/java/com/threadsdemo/
        |
        |-- JavaThreadDemoApplication.java            (entry point -- lists all demos)
        |
        |-- multithreading/                           PART 1: Thread Basics
        |     |-- Thread1.java                         (1) Creating a thread by extending Thread
        |     |-- Thread2.java                         (2) Creating a thread by implementing Runnable
        |     |-- Stack.java                           (3) synchronized keyword -- protecting shared data
        |     +-- BlockingQueue.java                   (4) wait()/notify() -- thread communication
        |
        +-- advanced/                                 PART 2-6: Advanced Concurrency
              |-- ReentrantLockDemo.java               (5) Flexible locks: tryLock, timed, Conditions
              |-- ReadWriteLockDemo.java               (6) Multiple readers, one writer (Cache)
              |-- AtomicDemo.java                      (7) Lock-free: CAS, AtomicInteger, LongAdder
              |-- ExecutorServiceDemo.java             (8) Thread pools: Fixed, Cached, Scheduled
              |-- CompletableFutureDemo.java           (9) Async pipelines: thenApply, allOf, exceptionally
              |-- SynchronizationUtilitiesDemo.java   (10) Semaphore, CountDownLatch, CyclicBarrier
              |-- BlockingQueueDemo.java              (11) Producer-Consumer with LinkedBlockingQueue
              |-- PhaserExchangerDemo.java            (12) Phaser (dynamic barrier) & Exchanger (swap)
              |-- ThreadLocalDemo.java                (13) Per-thread context & data-leak prevention
              +-- VirtualThreadsDemo.java             (14) Java 21+ lightweight threads
```

---

## Quick Start

```bash
# Prerequisites: Java 21+, Maven

git clone https://github.com/yunussid/java-thread-demo.git
cd java-thread-demo
mvn compile

# Run any demo:
java --enable-preview -cp target/classes com.threadsdemo.advanced.AtomicDemo
```

---

## Part 1 -- Thread Basics (multithreading/)

### What is a Thread in Java?

Every Java program starts with one thread -- the **main thread** that runs `main()`. To do multiple things at once, you create additional threads.

Java gives you two ways to create a thread. Both do the same thing -- the difference is style.

---

### 1.1 Creating a Thread by Extending `Thread`

**File:** `src/main/java/com/threadsdemo/multithreading/Thread1.java`

**What this class does:** Extends the `Thread` class and overrides `run()` with the code you want to execute on a separate thread.

```java
public class Thread1 extends Thread {

    public Thread1(String threadName) {
        super(threadName);    // give the thread a name for debugging
    }

    @Override
    public void run() {
        // This code runs on a NEW thread, not the main thread
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread " + Thread.currentThread().getName() + " -> i=" + i);
        }
    }
}
```

**How you use it:**
```java
Thread1 t = new Thread1("Worker-A");
t.start();   // creates a new OS thread and calls run() on it
```

**CRITICAL:** You must call `start()`, not `run()`.

| Call | What happens |
|------|-------------|
| `t.start()` | Creates a NEW thread, runs `run()` on it in parallel |
| `t.run()` | Runs `run()` on the CURRENT thread -- no multithreading at all |

**Downside:** Java only allows extending one class. If your class already extends something else, you cannot extend `Thread`.

---

### 1.2 Creating a Thread by Implementing `Runnable`

**File:** `src/main/java/com/threadsdemo/multithreading/Thread2.java`

**What this class does:** Implements the `Runnable` interface instead of extending `Thread`. This is the **preferred approach** because:
- Your class is free to extend another class
- It separates "the task" (Runnable) from "the thread" (Thread)

```java
public class Thread2 implements Runnable {

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread " + Thread.currentThread().getName() + " -> i=" + i);
        }
    }
}
```

**How you use it:**
```java
Thread t = new Thread(new Thread2(), "Worker-B");
t.start();
```

**Which should you use?**

| | `extends Thread` | `implements Runnable` |
|-|-------------------|----------------------|
| Can extend another class? | No | Yes |
| Separation of concerns? | No (task + thread are mixed) | Yes (task is separate) |
| Can reuse with ExecutorService? | No | Yes |
| Recommended? | For quick demos only | **Yes -- use this in real code** |

---

### 1.3 The `synchronized` Keyword -- Protecting Shared Data

**File:** `src/main/java/com/threadsdemo/multithreading/Stack.java`

**The problem this solves:**

When two threads access the same `Stack` at the same time, things can go wrong:

```
Thread A:  reads top = 2
Thread B:  reads top = 2         (at the same time!)
Thread A:  sets top = 3, writes value to array[3]
Thread B:  sets top = 3, writes value to array[3]   (OVERWRITES Thread A's value!)

Result: One value is silently lost. The stack is corrupted.
```

**The solution: `synchronized`**

Adding `synchronized` to a method means: **only one thread can execute this method at a time.** All other threads wait in line.

```java
public synchronized boolean push(int element) {
    if (isFull()) return false;
    ++top;
    try { Thread.sleep(1000); } catch (Exception e) {}   // simulate slow work
    array[top] = element;
    return true;
}

public synchronized int pop() {
    if (isEmpty()) return Integer.MIN_VALUE;
    int obj = array[top];
    array[top] = Integer.MIN_VALUE;
    try { Thread.sleep(1000); } catch (Exception e) {}
    --top;
    return obj;
}
```

**How synchronized works internally:**

Every Java object has an invisible **monitor lock** (also called intrinsic lock). When a thread enters a `synchronized` method:

```
Thread A enters push()
  |
  +-> Acquires the lock on "this" Stack object
  |
  +-> Executes the method body
  |
  +-> Releases the lock when the method returns

Thread B tries to enter push() or pop()
  |
  +-> Sees the lock is held by Thread A
  |
  +-> WAITS (blocked) until Thread A releases it
```

**Key insight:** `synchronized` locks the **object**, not the method. If `push()` and `pop()` are both `synchronized` on the same object, calling `push()` also blocks `pop()` and vice versa.

---

### 1.4 `wait()` and `notify()` -- Thread Communication

**File:** `src/main/java/com/threadsdemo/multithreading/BlockingQueue.java`

**The problem:** `synchronized` prevents two threads from corrupting data, but what if one thread needs to **wait for a condition** that another thread will make true?

Example: A consumer thread wants to remove an item, but the queue is empty. It should wait for a producer to add something.

**The solution: `wait()` and `notifyAll()`**

```java
public boolean add(int item) {
    synchronized (q) {
        while (q.size() == capacity) {
            q.wait();        // "I cannot proceed. I will SLEEP and release the lock."
        }
        q.add(item);
        q.notifyAll();       // "I changed something. WAKE UP everyone who is waiting."
        return true;
    }
}

public int remove() {
    synchronized (q) {
        while (q.isEmpty()) {
            q.wait();        // "Queue is empty. I will sleep until someone adds something."
        }
        int item = q.poll();
        q.notifyAll();       // "I removed an item. Wake up producers who were waiting for space."
        return item;
    }
}
```

**Step by step:**

```
1. Consumer calls remove()
2. Queue is empty -> consumer calls q.wait()
3. Consumer RELEASES the lock and goes to sleep
4. Producer enters add(), acquires the lock
5. Producer adds an item and calls q.notifyAll()
6. Consumer wakes up, re-acquires the lock
7. Consumer checks while loop again -- queue is not empty now
8. Consumer removes the item and returns
```

**Why `while` and not `if`?**

```java
while (q.isEmpty()) {   // CORRECT -- re-check after waking up
    q.wait();
}

if (q.isEmpty()) {      // WRONG -- another thread may have consumed the item
    q.wait();           // between your wake-up and your re-acquisition of the lock
}
```

This is called a **spurious wakeup** -- the thread can wake up even without `notify()` being called. The `while` loop handles this safely.

> This is the **educational** low-level approach. In production, use `java.util.concurrent.LinkedBlockingQueue` which handles all of this for you (see Part 4).

---

## Part 2 -- Locks & Synchronization (advanced/)

### 2.1 `ReentrantLock` -- Flexible Locking

**File:** `src/main/java/com/threadsdemo/advanced/ReentrantLockDemo.java`

**Why not just use `synchronized`?**

`synchronized` is simple but limited. It blocks forever -- you cannot say "try for 2 seconds, then give up." `ReentrantLock` adds features:

| Feature | `synchronized` | `ReentrantLock` |
|---------|---------------|-----------------|
| Try without blocking | No | `tryLock()` returns false immediately |
| Timed wait | No | `tryLock(2, SECONDS)` waits up to 2s |
| Interruptible wait | No | `lockInterruptibly()` can be interrupted |
| Fair ordering (FIFO) | No | `new ReentrantLock(true)` |
| Multiple conditions | No | `lock.newCondition()` |

**The pattern -- ALWAYS unlock in `finally`:**

```java
ReentrantLock lock = new ReentrantLock();

lock.lock();          // acquire
try {
    // critical section (only one thread at a time)
    counter++;
} finally {
    lock.unlock();    // ALWAYS release in finally -- even if an exception is thrown
}
```

If you forget `unlock()`, other threads will wait **forever**. That is a deadlock.

**What "Reentrant" means:**

The same thread can acquire the lock **multiple times** without deadlocking itself:

```java
lock.lock();          // count = 1
lock.lock();          // count = 2 (same thread, no deadlock)
lock.unlock();        // count = 1
lock.unlock();        // count = 0 (fully released, other threads can enter)
```

`synchronized` also has this property -- a thread can enter two `synchronized` methods on the same object without deadlocking.

**Demos inside the class:**

| Method | What it shows |
|--------|--------------|
| `basicLockDemo()` | Two threads safely incrementing a shared counter to exactly 2000 |
| `tryLockDemo()` | One thread holds the lock for 2 seconds. Another calls `tryLock()` -- gets `false` immediately instead of blocking |
| `timedLockDemo()` | Thread tries `tryLock(2, SECONDS)` while another holds it for 3 seconds. Times out after 2s |
| `conditionDemo()` | `Condition` variables (`await`/`signal`) replace `wait`/`notify` in a bounded buffer. Multiple conditions on one lock |

---

### 2.2 `ReadWriteLock` -- Many Readers, One Writer

**File:** `src/main/java/com/threadsdemo/advanced/ReadWriteLockDemo.java`

**The problem:** A cache is read by 100 threads but written by 1 thread. With `synchronized`, all 100 readers block each other even though reading is safe.

**The solution:** `ReadWriteLock` allows:
- **Multiple readers at the same time** (read lock is shared)
- **Only one writer at a time** (write lock is exclusive -- blocks readers too)

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// READING -- many threads can do this at once
rwLock.readLock().lock();
try {
    return cache.get(key);
} finally {
    rwLock.readLock().unlock();
}

// WRITING -- exclusive access, blocks all readers and other writers
rwLock.writeLock().lock();
try {
    cache.put(key, value);
} finally {
    rwLock.writeLock().unlock();
}
```

The demo implements a full thread-safe `Cache` class with `get()`, `put()`, `remove()`, `getAll()`, and `clear()`.

---

### 2.3 Atomic Classes -- Lock-Free Thread Safety

**File:** `src/main/java/com/threadsdemo/advanced/AtomicDemo.java`

**The problem:** Locks work but they are slow -- threads wait in line. For simple counters, there is a faster way.

**The solution:** Atomic classes use **CAS (Compare-And-Swap)** -- a single CPU instruction that atomically reads, compares, and writes. No lock needed.

**How CAS works:**

```
Thread A: "If counter is currently 10, change it to 11"

CPU checks:
  - Is counter == 10? YES -> writes 11. Done.
  - Is counter == 10? NO (someone changed it) -> FAILS. Thread A retries.
```

No thread ever blocks. If there is a conflict, the thread simply retries. This is called **lock-free** programming.

| Class | What it does |
|-------|-------------|
| `AtomicInteger` | Thread-safe `int`. `incrementAndGet()`, `compareAndSet()` |
| `AtomicLong` | Thread-safe `long` |
| `AtomicReference<V>` | Thread-safe object reference swap |
| `LongAdder` | Like `AtomicLong` but much faster under high contention (uses internal striping) |

```java
AtomicInteger counter = new AtomicInteger(0);

// Thread-safe increment -- no lock needed
counter.incrementAndGet();   // returns new value

// CAS -- "If currently 100, change to 200"
boolean success = counter.compareAndSet(100, 200);
```

**Demos inside the class:**

| Method | What it shows |
|--------|--------------|
| `atomicIntegerDemo()` | 10 threads x 1000 increments = exactly 10,000 (no race condition) |
| `atomicReferenceDemo()` | CAS on a `User` object -- only one thread's update wins |
| `compareAndSetDemo()` | Raw CAS, `updateAndGet(v -> v * 2)`, `accumulateAndGet(50, Integer::sum)` |
| `longAdderDemo()` | Benchmark: `LongAdder` vs `AtomicLong` with 10 threads x 1,000,000 increments. LongAdder is significantly faster |

---

## Part 3 -- Executors & Thread Pools (advanced/)

### 3.1 `ExecutorService` -- Reusing Threads

**File:** `src/main/java/com/threadsdemo/advanced/ExecutorServiceDemo.java`

**The problem:** Creating a new `Thread` for every task is expensive. Each thread uses ~1MB of memory and takes time to create/destroy.

**The solution:** A **thread pool** creates N threads once and reuses them for thousands of tasks:

```
Without pool:  Task 1 -> create Thread -> run -> destroy Thread
               Task 2 -> create Thread -> run -> destroy Thread
               Task 3 -> create Thread -> run -> destroy Thread
               (3 threads created and destroyed)

With pool:     Pool has 2 threads ready
               Task 1 -> Thread-1 runs it
               Task 2 -> Thread-2 runs it
               Task 3 -> Thread-1 finishes Task 1, picks up Task 3
               (2 threads reused for 3 tasks)
```

| Pool Type | When to Use |
|-----------|-------------|
| `newFixedThreadPool(N)` | You know the workload. N threads always alive |
| `newCachedThreadPool()` | Many short tasks. Threads created on demand, reused for 60s |
| `newSingleThreadExecutor()` | Guaranteed sequential execution. One thread |
| `newScheduledThreadPool(N)` | Delayed or periodic tasks (like cron) |
| `ThreadPoolExecutor(...)` | Full control: core size, max size, queue, rejection policy |

**Proper shutdown:**
```java
executor.shutdown();                             // stop accepting new tasks
executor.awaitTermination(5, TimeUnit.SECONDS);  // wait for running tasks to finish
```

**Demos inside the class:**

| Method | What it shows |
|--------|--------------|
| `fixedThreadPoolDemo()` | 6 tasks on 3 threads -- tasks 4, 5, 6 wait in a queue |
| `cachedThreadPoolDemo()` | 5 tasks -- pool creates 5 threads on demand |
| `singleThreadExecutorDemo()` | 3 tasks always run on the same thread, in order |
| `scheduledThreadPoolDemo()` | One-shot 1s delay + periodic task every 500ms |
| `futureDemo()` | `submit(Callable)` returns `Future`. Call `.get()` to block and get the result |
| `invokeAllDemo()` | Submit 5 `Callable`s, wait for all results at once |
| `customThreadPoolDemo()` | `ThreadPoolExecutor` with core=2, max=4, bounded queue=10, `CallerRunsPolicy` |

---

### 3.2 `CompletableFuture` -- Async Pipelines

**File:** `src/main/java/com/threadsdemo/advanced/CompletableFutureDemo.java`

**The problem with `Future.get()`:** It blocks your thread. You are back to sequential execution.

**The solution:** `CompletableFuture` lets you chain async operations without blocking:

```java
CompletableFuture
    .supplyAsync(() -> fetchUser())           // runs on background thread
    .thenApply(user -> user.toUpperCase())    // transform result (still async)
    .thenAccept(name -> log.info(name))       // consume result (still async)
    .exceptionally(ex -> fallback());          // handle errors
```

**Mental model -- think of it as a pipeline:**

```
supplyAsync("hello")              -- start with a value (async)
  |
  +-> thenApply(toUpperCase)      -- transform: "HELLO"
  |
  +-> thenApply(+ " WORLD")      -- transform: "HELLO WORLD"
  |
  +-> thenAccept(print)           -- side-effect: prints "HELLO WORLD"
```

| Operation | What it does | Analogy |
|-----------|-------------|---------|
| `supplyAsync(fn)` | Start async task that returns a value | Start the pipeline |
| `thenApply(fn)` | Transform the result | `map` in streams |
| `thenCompose(fn)` | Chain another async call | `flatMap` in streams |
| `thenCombine(other, fn)` | Combine two independent futures | `zip` |
| `allOf(f1, f2, f3)` | Wait for ALL to complete | `Promise.all` in JavaScript |
| `anyOf(f1, f2, f3)` | Complete when ANY finishes | `Promise.race` in JavaScript |
| `exceptionally(fn)` | Handle error with fallback | `catch` |
| `handle(fn)` | Handle both success and error | `then + catch` |

**Real-world example -- Parallel API calls:**

```java
CompletableFuture<String> user   = CompletableFuture.supplyAsync(() -> fetchUser());     // 200ms
CompletableFuture<String> orders = CompletableFuture.supplyAsync(() -> fetchOrders());   // 300ms
CompletableFuture<String> recs   = CompletableFuture.supplyAsync(() -> fetchRecs());     // 250ms

// All 3 run in PARALLEL. Total time = max(200, 300, 250) = ~300ms, not 750ms
CompletableFuture.allOf(user, orders, recs)
    .thenApply(v -> buildDashboard(user.join(), orders.join(), recs.join()));
```

---

## Part 4 -- Synchronization Utilities (advanced/)

### 4.1 `Semaphore` -- Limit Concurrent Access

**File:** `src/main/java/com/threadsdemo/advanced/SynchronizationUtilitiesDemo.java` -> `semaphoreDemo()`

**What it is:** A counter of "permits." A thread must `acquire()` a permit to proceed. If none are available, it blocks. When done, it `release()`s the permit.

**Real-world use case:** Your microservice talks to a database that can handle only 3 connections. A `Semaphore(3)` ensures no more than 3 threads use the database at once.

```java
Semaphore connectionPool = new Semaphore(3, true);   // 3 permits, fair (FIFO)

connectionPool.acquire();    // blocks if all 3 permits are taken
try {
    queryDatabase();
} finally {
    connectionPool.release();  // return the permit
}
```

**Key difference from locks:** A Semaphore has **no owner**. Any thread can release a permit, even if it did not acquire it. A lock can only be released by the thread that acquired it.

| Semaphore(1) | Lock |
|-------------|------|
| Acts like a lock (1 permit = 1 thread) | Also 1 thread at a time |
| Any thread can release | Only the owner can unlock |
| No reentrancy | ReentrantLock supports reentrancy |

---

### 4.2 `CountDownLatch` vs `CyclicBarrier`

**File:** `src/main/java/com/threadsdemo/advanced/SynchronizationUtilitiesDemo.java`

These two are the most confused classes in the entire Java concurrency API. Here is the difference:

| | `CountDownLatch` | `CyclicBarrier` |
|-|-----------------|-----------------|
| Reusable? | **No** -- one-shot | **Yes** -- resets automatically |
| Who waits? | One or more threads call `await()` | All participating threads call `await()` |
| Who counts? | Workers call `countDown()` | The barrier counts arrivals automatically |
| Use case | "Wait until N events happen" | "All N threads must reach this point before anyone proceeds" |

#### `CountDownLatch` -- The One-Time Gate

**Method:** `countDownLatchDemo()`

**Use case:** Main thread waits for 5 subsystems (Database, Kafka, Redis, Config, HealthCheck) to finish booting before accepting traffic.

```java
CountDownLatch readyLatch = new CountDownLatch(5);

// Each subsystem thread, when done booting:
readyLatch.countDown();       // "I am ready" (count: 5 -> 4 -> 3 -> ... -> 0)

// Main thread:
readyLatch.await();           // blocks until count reaches 0
acceptTraffic();
```

#### `CyclicBarrier` -- The Reusable Meeting Point

**Method:** `cyclicBarrierDemo()`

**Use case:** Process 1,000,000 records in 4 chunks. All 4 threads must finish Phase 1 before any can start Phase 2.

```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> {
    aggregateResults();       // runs once when all 4 arrive
});

// Each worker:
processChunk();
barrier.await();              // wait for all 4 -> aggregation runs -> barrier RESETS

transformChunk();
barrier.await();              // works again for Phase 2!
```

---

### 4.3 `BlockingQueue` -- Producer-Consumer

**File:** `src/main/java/com/threadsdemo/advanced/BlockingQueueDemo.java`

**What it is:** A thread-safe queue that blocks the producer if full and blocks the consumer if empty. No `synchronized`, `wait()`, or `notify()` needed -- it handles everything internally.

```java
BlockingQueue<Order> queue = new LinkedBlockingQueue<>(5);   // bounded, max 5 items

// Producer:
queue.put(order);     // BLOCKS if queue has 5 items (back-pressure)

// Consumer:
Order o = queue.take();   // BLOCKS if queue is empty (no busy-waiting)
```

**Graceful shutdown with Poison Pill:**

```java
Order POISON_PILL = new Order(-1, "SHUTDOWN");

// After all producers finish:
for (int i = 0; i < consumerCount; i++)
    queue.put(POISON_PILL);

// In consumer loop:
Order order = queue.take();
if (order == POISON_PILL) break;
```

---

### 4.4 `Phaser` -- Dynamic Multi-Phase Barrier

**File:** `src/main/java/com/threadsdemo/advanced/PhaserExchangerDemo.java` -> `phaserDemo()`

**What it is:** Like `CyclicBarrier`, but the number of participating threads can **change at runtime**.

| Method | Meaning |
|--------|---------|
| `register()` | "I am joining this phase" |
| `arriveAndAwaitAdvance()` | "I am done, wait for everyone" |
| `arriveAndDeregister()` | "I am done AND leaving -- do not wait for me next phase" |

**Demo:** 4 workers start in Phase 0. Worker-4 deregisters after Phase 0. Worker-3 deregisters after Phase 1. Only Workers 1 and 2 finish Phase 2.

---

### 4.5 `Exchanger` -- Two-Thread Swap

**File:** `src/main/java/com/threadsdemo/advanced/PhaserExchangerDemo.java` -> `exchangerDemo()`

**What it is:** A synchronization point where exactly two threads swap objects.

**Use case -- Dual buffering:** Thread A fills buffer, Thread B empties buffer. When both are done, they exchange buffers. One buffer is always being filled while the other is consumed.

```java
Exchanger<List<String>> exchanger = new Exchanger<>();

// Filler:  buffer = exchanger.exchange(buffer);  // give full, receive empty
// Drainer: buffer = exchanger.exchange(buffer);  // give empty, receive full
```

---

## Part 5 -- ThreadLocal (advanced/)

**File:** `src/main/java/com/threadsdemo/advanced/ThreadLocalDemo.java`

**What it is:** A variable that is **local to each thread**. Each thread has its own copy. No sharing, no race conditions.

**Real-world use case:** In Spring, `SecurityContextHolder` uses `ThreadLocal` to store the logged-in user. Any code in the request chain can call `SecurityContextHolder.getContext()` without the user being passed as a parameter.

```
Controller -> Service -> Repository -> AuditLogger
     ^                                    ^
     +---- ThreadLocal stores userId -----+
           (no parameter passing needed)
```

```java
private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

CONTEXT.set("alice");          // set in this thread only
CONTEXT.get();                 // "alice" (only in this thread)
CONTEXT.remove();              // clean up
```

### WARNING: ThreadLocal + Thread Pools = Data Leak

Thread pools **reuse threads**. If you forget `remove()`, the next request handled by the same thread sees the previous user's data:

```
Request-1 (Thread-1): CONTEXT.set("alice")  -- forgot remove()!
Request-2 (Thread-1): CONTEXT.get() -> "alice"    BUG! This is a different user!
```

**Always call `.remove()` in a `finally` block:**

```java
try {
    CONTEXT.set("alice");
    // handle request
} finally {
    CONTEXT.remove();   // ALWAYS clean up
}
```

**Demos inside the class:**

| Method | What it shows |
|--------|--------------|
| `basicIsolationDemo()` | Two threads (alice, bob) with different contexts that never interfere |
| `dataLeakDemo()` | **Deliberately buggy** -- shows the leak in a single-thread pool |
| `safeUsageDemo()` | Same scenario with `finally { remove() }` -- no leak |

---

## Part 6 -- Virtual Threads (Java 21+)

**File:** `src/main/java/com/threadsdemo/advanced/VirtualThreadsDemo.java`

**The problem:** Platform threads (the normal kind) use ~1MB of memory each and are managed by the operating system. You can create maybe a few thousand before running out of memory.

**The solution:** Virtual threads are managed by the **JVM**, use only a few KB, and you can create **millions** of them:

| | Platform Thread | Virtual Thread |
|-|----------------|----------------|
| Managed by | OS | JVM |
| Memory | ~1 MB | ~few KB |
| Max practical count | ~thousands | **millions** |
| Best for | CPU-bound work | **I/O-bound work** (HTTP calls, DB queries) |

```java
// Create a virtual thread
Thread.startVirtualThread(() -> doWork());

// Executor (recommended for production)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> doWork());
}
```

**Migration from platform threads:**
```java
// Before:
Executors.newFixedThreadPool(100)

// After (just change one line):
Executors.newVirtualThreadPerTaskExecutor()
```

**Best Practices:**
- Use for I/O-bound work (HTTP, DB, file I/O)
- Do NOT use for CPU-bound work (math, image processing)
- Do NOT pool virtual threads -- they are cheap, just create new ones
- Avoid `synchronized` blocks (pins the carrier OS thread) -- use `ReentrantLock` instead

**Demos inside the class:**

| Method | What it shows |
|--------|--------------|
| `creationDemo()` | Three ways to create virtual threads + platform thread comparison |
| `executorDemo()` | `newVirtualThreadPerTaskExecutor()` -- one virtual thread per task |
| `scalabilityDemo()` | 10,000 tasks: virtual threads vs platform pool(100). Virtual threads ~10x faster |
| `ioTaskDemo()` | 100 concurrent "HTTP requests" (500ms each) complete in ~500ms total |

---

## Concept Map -- When to Use What

```
I need to...                              -> Use this
--------------------------------------------------------------
Run code on a separate thread             -> Thread / Runnable
Protect shared data (simple)              -> synchronized
Protect shared data (flexible)            -> ReentrantLock
Try lock without blocking                 -> ReentrantLock.tryLock()
Many readers, few writers                 -> ReadWriteLock
Lock-free counter                         -> AtomicInteger / LongAdder
Reuse threads for many tasks              -> ExecutorService (thread pool)
Async pipeline with chaining              -> CompletableFuture
Limit concurrent access to N              -> Semaphore
Wait for N events (one-time)              -> CountDownLatch
Sync N threads at reusable barrier        -> CyclicBarrier
Dynamic barrier (add/remove threads)      -> Phaser
Swap data between 2 threads               -> Exchanger
Decouple producer from consumer           -> BlockingQueue
Per-thread context (user ID, request ID)  -> ThreadLocal
Massive I/O concurrency (millions)        -> Virtual Threads (Java 21+)
```

---

## Running All Demos

Each class has its own `main()` method. Run any one:

```bash
mvn compile

java --enable-preview -cp target/classes com.threadsdemo.advanced.ReentrantLockDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ReadWriteLockDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.AtomicDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ExecutorServiceDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.CompletableFutureDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.SynchronizationUtilitiesDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.BlockingQueueDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.PhaserExchangerDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ThreadLocalDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.VirtualThreadsDemo
```

---

## License

See [LICENSE](LICENSE) file.
