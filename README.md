# ☕ Java Threading & Concurrency — Complete Guide

A **hands-on**, runnable reference for every major Java concurrency concept — from basic `Thread` creation all the way to `Phaser`, `Exchanger`, `ThreadLocal`, and Java 21+ Virtual Threads.

Every section below contains:
1. **What it is** — plain-English explanation.
2. **When to use it** — real-world scenario.
3. **How it works** — the key API calls, with a code snippet.
4. **▶ Run it** — the exact class you can execute.

---

## 📁 Project Structure

```
java-thread-demo/
├── pom.xml
├── README.md
└── src/main/java/com/threadsdemo/
    ├── JavaThreadDemoApplication.java          ← Project entry-point / index
    │
    ├── multithreading/                         ← Thread basics
    │   ├── Thread1.java                        ← Creating threads by extending Thread
    │   ├── Thread2.java                        ← Creating threads by implementing Runnable
    │   ├── Stack.java                          ← Synchronized stack (shared resource)
    │   └── BlockingQueue.java                  ← Hand-rolled blocking queue (wait/notify)
    │
    └── advanced/                               ← Advanced concurrency
        ├── ReentrantLockDemo.java              ← Lock, tryLock, timed lock, Conditions
        ├── ReadWriteLockDemo.java              ← Shared reads, exclusive writes (Cache)
        ├── AtomicDemo.java                     ← CAS, AtomicInteger, LongAdder
        ├── ExecutorServiceDemo.java            ← Thread pools & Future
        ├── CompletableFutureDemo.java          ← Async chaining, combining, error handling
        ├── SynchronizationUtilitiesDemo.java   ← Semaphore, CountDownLatch, CyclicBarrier
        ├── BlockingQueueDemo.java              ← Producer-Consumer with LinkedBlockingQueue
        ├── PhaserExchangerDemo.java            ← Phaser (dynamic barrier) & Exchanger (buffer swap)
        ├── ThreadLocalDemo.java                ← Context isolation & data-leak prevention
        └── VirtualThreadsDemo.java             ← Java 21+ lightweight threads
```

---

## ⚡ Quick Start

```bash
# Clone & build
git clone <repo-url>
cd java-thread-demo
mvn compile

# Run any demo (replace the class name)
java --enable-preview -cp target/classes com.threadsdemo.advanced.SynchronizationUtilitiesDemo
```

> **Java 21+** is required. The project uses `--enable-preview` for latest language features.

---

## 🧵 Part 1 — Thread Basics

### 1.1 Creating Threads by Extending `Thread`

**▶ Class:** `multithreading/Thread1.java`

The simplest way to create a thread. Extend `Thread` and override `run()`.

```java
public class Thread1 extends Thread {
    public Thread1(String threadName) {
        super(threadName);
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Thread " + Thread.currentThread().getName() + " -> i=" + i);
        }
    }
}
```

**How to use it:**
```java
Thread1 t = new Thread1("Worker");
t.start();  // start(), not run()!
```

> ⚠️ Calling `run()` directly executes on the *current* thread. Always use `start()`.

---

### 1.2 Creating Threads by Implementing `Runnable`

**▶ Class:** `multithreading/Thread2.java`

Preferred approach — keeps your class free to extend something else.

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

**How to use it:**
```java
Thread t = new Thread(new Thread2(), "MyRunnable");
t.start();
```

---

### 1.3 Synchronized Shared Resource — `Stack`

**▶ Class:** `multithreading/Stack.java`

A shared `Stack` used by multiple threads. The `push()` and `pop()` methods are `synchronized` to prevent race conditions.

```java
public synchronized boolean push(int element) {
    if (isFull()) return false;
    ++top;
    Thread.sleep(1000);   // simulate slow work
    array[top] = element;
    return true;
}
```

**Key insight:** Without `synchronized`, two threads could both see `top=2`, both increment to `3`, and one value would overwrite the other.

---

### 1.4 Hand-Rolled `BlockingQueue` (wait / notify)

**▶ Class:** `multithreading/BlockingQueue.java`

A custom bounded queue built with `synchronized`, `wait()`, and `notifyAll()` — the classic low-level producer-consumer mechanism.

```java
public boolean add(int item) {
    synchronized (q) {
        while (q.size() == capacity) {
            q.wait();           // block producer until space is available
        }
        q.add(item);
        q.notifyAll();          // wake up consumers
        return true;
    }
}
```

| Method | Behaviour |
|--------|-----------|
| `add(item)` | Blocks if queue is full |
| `remove()` | Blocks if queue is empty |

> This is the **educational version**. For production code, use `java.util.concurrent.LinkedBlockingQueue` (see Part 4).

---

## 🔒 Part 2 — Locks & Synchronization

### 2.1 `ReentrantLock` — Flexible Locking

**▶ Class:** `advanced/ReentrantLockDemo.java`

`ReentrantLock` gives you everything `synchronized` does, plus:

| Feature | `synchronized` | `ReentrantLock` |
|---------|---------------|-----------------|
| Try without blocking | ❌ | `tryLock()` |
| Timed wait | ❌ | `tryLock(2, SECONDS)` |
| Interruptible wait | ❌ | `lockInterruptibly()` |
| Fair ordering (FIFO) | ❌ | `new ReentrantLock(true)` |
| Multiple conditions | ❌ | `lock.newCondition()` |

**Pattern — always unlock in `finally`:**
```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();  // ALWAYS release in finally!
}
```

**Demos inside the class:**

| Demo | What it shows |
|------|--------------|
| `basicLockDemo()` | Two threads safely incrementing a shared counter |
| `tryLockDemo()` | Non-blocking lock attempt — returns `false` instead of blocking |
| `timedLockDemo()` | Wait up to 2 seconds for the lock, then give up |
| `conditionDemo()` | `Condition` variables (`await`/`signal`) replacing `wait`/`notify` in a bounded buffer |

---

### 2.2 `ReadWriteLock` — Shared Reads, Exclusive Writes

**▶ Class:** `advanced/ReadWriteLockDemo.java`

When reads vastly outnumber writes, a single mutex is wasteful — you are blocking readers from each other for no reason.

`ReadWriteLock` allows:
- **Multiple readers simultaneously** (read lock is shared).
- **Only one writer at a time** (write lock is exclusive, blocks readers too).

**Real-world analogy:** A cache. 100 threads reading the cache should not block each other. Only when an entry is updated should everyone wait.

```java
// READING — many threads can do this at once
rwLock.readLock().lock();
try {
    return cache.get(key);
} finally {
    rwLock.readLock().unlock();
}

// WRITING — exclusive access
rwLock.writeLock().lock();
try {
    cache.put(key, value);
} finally {
    rwLock.writeLock().unlock();
}
```

The demo implements a full `Cache` class with `get()`, `put()`, `remove()`, `getAll()`, and `clear()` methods backed by `ReentrantReadWriteLock`.

---

### 2.3 Atomic Classes — Lock-Free Thread Safety

**▶ Class:** `advanced/AtomicDemo.java`

Atomic classes use **CAS (Compare-And-Swap)** at the hardware level — no locks needed.

| Class | Use |
|-------|-----|
| `AtomicInteger` / `AtomicLong` | Thread-safe counters |
| `AtomicReference<V>` | Thread-safe object reference swaps |
| `LongAdder` | High-contention counter (faster than AtomicLong) |

**CAS in action:**
```java
// "If the value is currently 100, change it to 200"
boolean success = value.compareAndSet(100, 200);
// Returns true if swap happened, false if someone else changed it first
```

**Demos inside the class:**

| Demo | What it shows |
|------|--------------|
| `atomicIntegerDemo()` | 10 threads x 1000 increments = exactly 10,000 (no race) |
| `atomicReferenceDemo()` | CAS on a `User` object — only one thread update wins |
| `compareAndSetDemo()` | Raw CAS, `updateAndGet()`, `accumulateAndGet()` |
| `longAdderDemo()` | Benchmark: `LongAdder` vs `AtomicLong` under 10-thread contention |

---

## ⚙️ Part 3 — Executors & Thread Pools

### 3.1 `ExecutorService` — Thread Pool Management

**▶ Class:** `advanced/ExecutorServiceDemo.java`

Creating a new `Thread` per task is expensive. Executors manage a **pool of reusable threads**.

| Pool Type | When To Use |
|-----------|-------------|
| `newFixedThreadPool(N)` | Known, steady workload — N threads always alive |
| `newCachedThreadPool()` | Many short-lived tasks — threads are created on demand, cached 60s |
| `newSingleThreadExecutor()` | Guaranteed sequential execution (one thread) |
| `newScheduledThreadPool(N)` | Delayed or periodic tasks (cron-like) |
| `ThreadPoolExecutor(...)` | Full control over core size, max size, queue, rejection policy |

**Proper shutdown:**
```java
executor.shutdown();                             // no new tasks accepted
executor.awaitTermination(5, TimeUnit.SECONDS);  // wait for running tasks
```

**Demos inside the class:**

| Demo | What it shows |
|------|--------------|
| `fixedThreadPoolDemo()` | 6 tasks on 3 threads — tasks queue up |
| `cachedThreadPoolDemo()` | 5 tasks — pool creates threads on demand |
| `singleThreadExecutorDemo()` | 3 tasks always run on the same thread |
| `scheduledThreadPoolDemo()` | One-shot delay + periodic task every 500ms |
| `futureDemo()` | `submit(Callable)` then `Future.get()` to retrieve a result |
| `invokeAllDemo()` | Submit a list of `Callable`s, wait for all results |
| `customThreadPoolDemo()` | `ThreadPoolExecutor` with custom core/max/queue/rejection policy |

---

### 3.2 `CompletableFuture` — Async Pipeline

**▶ Class:** `advanced/CompletableFutureDemo.java`

`CompletableFuture` is Java's answer to JavaScript Promises — non-blocking, composable async pipelines.

**Mental model — a pipeline of stages:**

```
supplyAsync("hello")
  -> thenApply(toUpperCase)     // "HELLO"
  -> thenApply(+ " WORLD")     // "HELLO WORLD"
  -> thenAccept(print)         // side-effect
```

| Operation | Analogy | Purpose |
|-----------|---------|---------|
| `thenApply(fn)` | `map` | Transform the result |
| `thenCompose(fn)` | `flatMap` | Chain another async call |
| `thenCombine(other, fn)` | `zip` | Combine two independent futures |
| `allOf(f1, f2, f3)` | `Promise.all` | Wait for ALL to complete |
| `anyOf(f1, f2, f3)` | `Promise.race` | Complete when ANY finishes |
| `exceptionally(fn)` | `catch` | Fallback on error |
| `handle(fn)` | `then+catch` | Handle both success and failure |

**Real-world example in the demo — Parallel API calls:**
```java
CompletableFuture<String> user   = CompletableFuture.supplyAsync(() -> fetchUser());
CompletableFuture<String> orders = CompletableFuture.supplyAsync(() -> fetchOrders());
CompletableFuture<String> recs   = CompletableFuture.supplyAsync(() -> fetchRecs());

// All 3 calls run in parallel — total time is about max(200, 300, 250) = ~300ms
CompletableFuture.allOf(user, orders, recs)
    .thenApply(v -> buildDashboard(user.join(), orders.join(), recs.join()));
```

---

## 🚦 Part 4 — Synchronization Utilities

### 4.1 `Semaphore` — The Traffic Controller

**▶ Class:** `advanced/SynchronizationUtilitiesDemo.java` -> `semaphoreDemo()`

A `Semaphore` maintains a set of **permits**. A thread must `acquire()` a permit before proceeding and `release()` it when done.

**Use Case:** Your microservice talks to a legacy database that can only handle **3 concurrent connections**. A `Semaphore(3)` prevents the system from overwhelming the database.

```java
Semaphore connectionPool = new Semaphore(3, true);  // 3 permits, fair ordering

// In each service thread:
connectionPool.acquire();    // blocks until a permit is available
try {
    // use the database connection
    executeQuery();
} finally {
    connectionPool.release();  // return the permit
}
```

**Key distinction:** Unlike a lock, a Semaphore has **no owner**. Any thread can release a permit, even if it did not acquire it. This makes it a *counting* mechanism, not a *mutual exclusion* mechanism.

**How the demo works:**
```
8 service threads compete for 3 permits

  Service-1  -> requesting connection
  Service-1  CONNECTED  (available: 2)
  Service-2  CONNECTED  (available: 1)
  Service-3  CONNECTED  (available: 0)
  Service-4  -> waiting (no permits left)
  Service-1  <- releases -> Service-4 gets in
```

---

### 4.2 `CountDownLatch` vs `CyclicBarrier` — The Sync Points

These are often confused. Here is the key difference:

| | `CountDownLatch` | `CyclicBarrier` |
|-|-----------------|-----------------|
| **Reusable?** | No, one-shot | Yes, resets automatically |
| **Who waits?** | One or more threads call `await()` | All participating threads call `await()` |
| **Who counts?** | Worker threads call `countDown()` | The barrier itself counts arrivals |
| **Use case** | Wait until N events happen | All N threads must reach this point |

#### 4.2a `CountDownLatch` — The One-Time Gate

**▶ Class:** `advanced/SynchronizationUtilitiesDemo.java` -> `countDownLatchDemo()`

**Use Case:** Initializing a complex service. The main thread waits for 5 background subsystems (**Database**, **Kafka**, **Redis**, **Config**, **HealthCheck**) to report "Ready" before it starts accepting traffic.

```java
CountDownLatch readyLatch = new CountDownLatch(5);

// Each subsystem thread, when ready:
readyLatch.countDown();      // decrement count

// Main thread:
readyLatch.await();          // blocks until count == 0
startAcceptingTraffic();
```

**How the demo works:**
```
  [Database]      booting (1050 ms)
  [Kafka]         booting (307 ms)
  [Redis-Cache]   booting (1033 ms)
  [Config-Server] booting (550 ms)
  [HealthCheck]   booting (946 ms)

  Main thread waiting...

  [Kafka]         READY  (remaining: 4)
  [Config-Server] READY  (remaining: 3)
  ...
  [Database]      READY  (remaining: 0)

  ALL SUBSYSTEMS READY — accepting traffic!
```

#### 4.2b `CyclicBarrier` — The Reusable Meeting Point

**▶ Class:** `advanced/SynchronizationUtilitiesDemo.java` -> `cyclicBarrierDemo()`

**Use Case:** Processing **1,000,000 records** in 4 chunks (250k each). Worker threads process independently but **wait at the barrier** to aggregate results before moving to the next phase.

```java
CyclicBarrier barrier = new CyclicBarrier(4, () -> {
    // This runs once when all 4 threads arrive
    aggregatePartialResults();
});

// In each worker thread:
processChunk();        // Phase 1 work
barrier.await();       // wait for all 4 -> aggregation runs

transformChunk();      // Phase 2 work
barrier.await();       // barrier resets and works again!
```

**How the demo works:**
```
  Chunk-0  Phase 1 — filtering records 1 to 250,000
  Chunk-1  Phase 1 — filtering records 250,001 to 500,000
  Chunk-2  Phase 1 — filtering records 500,001 to 750,000
  Chunk-3  Phase 1 — filtering records 750,001 to 1,000,000

  BARRIER — Phase 1 complete. Aggregated count: 501,284

  Chunk-0  Phase 2 — transforming records
  ...
  BARRIER — Phase 2 complete. Aggregated count: 499,102
```

---

### 4.3 `BlockingQueue` — The Producer-Consumer Backbone

**▶ Class:** `advanced/BlockingQueueDemo.java`

This is the **most critical utility for decoupled systems**. `BlockingQueue` is a thread-safe queue that:
- **Blocks the producer** if the queue is full (back-pressure).
- **Blocks the consumer** if the queue is empty (no busy-waiting).

**Use Case:** High-throughput order processing. Your API puts `Order` objects into a `LinkedBlockingQueue` during traffic spikes. A separate pool of worker threads consumes them at their own pace. This is essential for maintaining high **TPS** (Transactions Per Second).

```java
BlockingQueue<Order> queue = new LinkedBlockingQueue<>(5);  // bounded, capacity 5

// Producer thread:
queue.put(order);   // BLOCKS if queue is full (back-pressure!)

// Consumer thread:
Order o = queue.take();   // BLOCKS if queue is empty
processOrder(o);
```

**Graceful shutdown with Poison Pill pattern:**
```java
Order POISON_PILL = new Order(-1, "SHUTDOWN");

// After producers finish:
for (int i = 0; i < consumerCount; i++) {
    queue.put(POISON_PILL);  // one per consumer
}

// In consumer loop:
Order order = queue.take();
if (order == POISON_PILL) break;  // clean exit
```

**How the demo works:**
```
Queue capacity : 5  |  Producers: 2  |  Consumers: 3

  [Producer-1]  putting Order{id=100, item=Laptop}
  [Consumer-1]  processing Order{id=100, item=Laptop}
  ...
  All producers finished. Sending poison pills...
  [Consumer-2]  received shutdown signal
  [Consumer-1]  received shutdown signal
  [Consumer-3]  received shutdown signal

All orders processed. Queue is empty: true
```

---

### 4.4 `Phaser` — Dynamic Multi-Phase Synchronization

**▶ Class:** `advanced/PhaserExchangerDemo.java` -> `phaserDemo()`

`Phaser` is a **more flexible, reusable** version of both `CountDownLatch` and `CyclicBarrier`. Its killer feature: the number of participating threads (parties) can **change at runtime**.

| Method | Meaning |
|--------|---------|
| `phaser.register()` | Add a new party (thread) |
| `phaser.arriveAndAwaitAdvance()` | I am done, wait for everyone else |
| `phaser.arriveAndDeregister()` | I am done and leaving — do not wait for me next phase |

**Use Case:** Multi-phase parallel algorithm where the number of workers **fluctuates** based on task complexity in each phase.

```java
Phaser phaser = new Phaser(1);  // main thread registered

// Dynamically add workers
phaser.register();
new Thread(() -> {
    doPhase0();
    phaser.arriveAndAwaitAdvance();  // wait for everyone

    if (myWorkIsDone) {
        phaser.arriveAndDeregister();  // leave — fewer parties next phase
        return;
    }

    doPhase1();
    phaser.arriveAndAwaitAdvance();
}).start();
```

**How the demo works:**
```
Phase 0 — 4 workers parse data
  Worker-4 deregisters (data was trivial)

Phase 1 — 3 workers validate
  Worker-3 deregisters (validation complete)

Phase 2 — 2 workers finalize output

Phaser pipeline finished. isTerminated = true
```

---

### 4.5 `Exchanger` — Dual-Buffer Swap

**▶ Class:** `advanced/PhaserExchangerDemo.java` -> `exchangerDemo()`

A synchronization point where exactly **two threads** can **swap objects**.

**Use Case — Dual-buffering:**
- Thread A fills a buffer with data.
- Thread B processes/empties a buffer.
- When both are done, they **exchange** buffers and repeat.
- Result: one buffer is *always* being filled while the other is consumed — continuous throughput.

```java
Exchanger<List<String>> exchanger = new Exchanger<>();

// Filler thread:
buffer.addAll(data);
buffer = exchanger.exchange(buffer);  // give full buffer, receive empty one

// Drainer thread:
buffer = exchanger.exchange(buffer);  // give empty buffer, receive full one
process(buffer);
buffer.clear();
```

**How the demo works:**
```
  [Filler]   added R1-Item1 to R1-Item4
  [Filler]   buffer full — exchanging
  [Drainer]  received full buffer (4 items) — processing
  [Drainer]  buffer drained
  [Filler]   received empty buffer (size 0)
  ... (repeats for 3 rounds)
```

---

## 🧠 Part 5 — `ThreadLocal` — Context Isolation

**▶ Class:** `advanced/ThreadLocalDemo.java`

`ThreadLocal` provides variables that are **local to a specific thread**. Each thread has its own, independently initialized copy.

**Use Case:** In Spring, `SecurityContextHolder` uses `ThreadLocal` to store the current user authentication details. This allows **any service in that thread execution path** to access the user ID without passing it as a method parameter through every layer.

```
Controller -> Service -> Repository -> AuditLogger
     ^                                    ^
     +------ ThreadLocal stores userId ---+
           (no parameter passing needed)
```

```java
private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

// At the start of a request:
CONTEXT.set(new RequestContext("REQ-001", "alice"));

// Anywhere deep in the call stack:
String userId = CONTEXT.get().userId();  // "alice"
```

### WARNING: ThreadLocal + Thread Pools = Data Leak

Thread pools (**Tomcat**, **@Async**, **Executors**) **reuse threads**. If you forget to call `ThreadLocal.remove()`, data from one request can **leak into the next request** handled by the same thread.

**The demo proves this:**

```
--- DATA LEAK — Forgetting .remove() ---

  Request-1  set context -> User-Alice
  Request-2  read context -> User-Alice        <-- BUG! Wrong user!
```

**The fix — ALWAYS call `.remove()` in a `finally` block:**

```java
try {
    CONTEXT.set(new RequestContext("REQ-001", "alice"));
    // handle the request
} finally {
    CONTEXT.remove();  // ALWAYS clean up
}
```

```
--- SAFE USAGE ---

  Request-1  set context -> User-Alice
  Request-1  removed context in finally block
  Request-2  read context -> null              <-- Clean!
```

**Demos inside the class:**

| Demo | What it shows |
|------|--------------|
| `basicIsolationDemo()` | Two threads (alice, bob) with different contexts that never interfere, accessed through Controller to Service to Repository |
| `dataLeakDemo()` | **Deliberately buggy** — shows how forgetting `.remove()` leaks data across requests in a single-thread pool |
| `safeUsageDemo()` | Same scenario with proper `finally { remove() }` — no leaked data |

---

## 🪶 Part 6 — Virtual Threads (Java 21+)

**▶ Class:** `advanced/VirtualThreadsDemo.java`

Virtual threads are **lightweight threads managed by the JVM** (not the OS):

| | Platform Thread | Virtual Thread |
|-|----------------|----------------|
| Managed by | OS | JVM |
| Memory | ~1 MB stack | ~few KB |
| Max practical count | ~thousands | **millions** |
| Best for | CPU-bound work | **I/O-bound work** |

**Creation:**
```java
// Simple
Thread.startVirtualThread(() -> doWork());

// Named
Thread.ofVirtual().name("my-vt").start(() -> doWork());

// Executor (recommended for production)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> doWork());
}
```

**Demos inside the class:**

| Demo | What it shows |
|------|--------------|
| `creationDemo()` | Three ways to create virtual threads + platform thread comparison |
| `executorDemo()` | `newVirtualThreadPerTaskExecutor()` — one VT per task |
| `scalabilityDemo()` | 10,000 tasks: virtual threads vs platform thread pool(100) — VTs are ~10x faster |
| `ioTaskDemo()` | 100 concurrent HTTP requests (500ms each) complete in ~500ms total |

**Best Practices:**
- Use for I/O-bound operations (HTTP, DB, file I/O)
- Use `try-with-resources` with executor
- Do NOT use for CPU-bound operations
- Do NOT pool virtual threads (they are cheap — just create new ones)
- Avoid `synchronized` blocks (pins the carrier thread) — use `ReentrantLock` instead

**Migration from platform threads:**
```java
// Before:
Executors.newFixedThreadPool(N)

// After:
Executors.newVirtualThreadPerTaskExecutor()
```

---

## 🗺️ Concept Map — When to Use What

```
Need to...                              -> Use this
--------------------------------------------------------------
Limit concurrent access to N resources  -> Semaphore
Wait for N events to happen (one-time)  -> CountDownLatch
Sync N threads at a reusable barrier    -> CyclicBarrier
Dynamic barrier (add/remove threads)    -> Phaser
Swap data between exactly 2 threads     -> Exchanger
Decouple producer from consumer         -> BlockingQueue
Protect a critical section              -> synchronized / ReentrantLock
Many readers, few writers               -> ReadWriteLock
Lock-free counters                      -> AtomicInteger / LongAdder
Async pipelines with chaining           -> CompletableFuture
Reuse threads across many tasks         -> ExecutorService / ThreadPool
Per-thread context (e.g., user ID)      -> ThreadLocal
Massive I/O concurrency (millions)      -> Virtual Threads (Java 21+)
```

---

## 🧪 Running All Demos

Each class has its own `main()` method. Run any one independently:

```bash
# Build once
mvn compile

# Then run any demo:
java --enable-preview -cp target/classes com.threadsdemo.advanced.SynchronizationUtilitiesDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.BlockingQueueDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.PhaserExchangerDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ThreadLocalDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ReentrantLockDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ReadWriteLockDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.AtomicDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.ExecutorServiceDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.CompletableFutureDemo
java --enable-preview -cp target/classes com.threadsdemo.advanced.VirtualThreadsDemo
```

---

## 📜 License

See [LICENSE](LICENSE) file.
