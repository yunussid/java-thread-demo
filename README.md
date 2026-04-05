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

#### Demo 1: `basicLockDemo()` -- Safe Shared Counter

Two threads each increment a shared `counter` 1000 times. Without the lock, the result would be less than 2000 (race condition). With the lock, it is always exactly 2000.

```java
public void increment() {
    lock.lock();
    try {
        counter++;          // only one thread at a time
    } finally {
        lock.unlock();
    }
}
```

#### Demo 2: `tryLockDemo()` -- Non-Blocking Lock Attempt

**The problem with `lock.lock()`:** If the lock is held by another thread, your thread blocks forever. Sometimes you want to say "try once, and if you cannot get it, do something else."

**The code:**

```java
Thread holder = new Thread(() -> {
    lock.lock();
    try {
        System.out.println("Thread holding lock for 2 seconds...");
        Thread.sleep(2000);       // holds lock for 2 full seconds
    } finally {
        lock.unlock();
    }
}, "Holder");

Thread tryLocker = new Thread(() -> {
    Thread.sleep(500);            // wait 500ms so Holder grabs the lock first

    if (lock.tryLock()) {         // try ONCE, do NOT block
        try {
            System.out.println("tryLock succeeded!");
        } finally {
            lock.unlock();
        }
    } else {
        System.out.println("tryLock failed - lock not available");
    }
}, "TryLocker");
```

**What happens step by step:**

```
Time 0ms:     Holder starts, calls lock.lock() -- acquires the lock
Time 0ms:     Holder prints "holding lock for 2 seconds..." and sleeps

Time 500ms:   TryLocker wakes up, calls lock.tryLock()
              Lock is held by Holder -- tryLock() returns FALSE immediately
              TryLocker prints "tryLock failed - lock not available"
              TryLocker does NOT block -- it just moves on

Time 2000ms:  Holder wakes up, calls lock.unlock()
              (But TryLocker is already done -- it did not wait)
```

**Output:**
```
Thread holding lock for 2 seconds...
tryLock failed - lock not available
```

**Key insight:** `tryLock()` returns `false` immediately. `lock.lock()` would have blocked TryLocker for 1.5 seconds. This is useful when you have a fallback (e.g., return a cached value instead of waiting for the database).

#### Demo 3: `timedLockDemo()` -- Wait Up To a Timeout

**The problem:** `tryLock()` gives up instantly. Sometimes you want to wait for a little while, but not forever.

**The code:**

```java
Thread holder = new Thread(() -> {
    lock.lock();
    try {
        System.out.println("Holder: Got lock, holding for 3 seconds...");
        Thread.sleep(3000);       // holds lock for 3 seconds
    } finally {
        lock.unlock();
        System.out.println("Holder: Released lock");
    }
});

Thread waiter = new Thread(() -> {
    Thread.sleep(500);            // let Holder grab the lock first

    System.out.println("Waiter: Trying to acquire lock with 2 second timeout...");

    if (lock.tryLock(2, TimeUnit.SECONDS)) {    // wait UP TO 2 seconds
        try {
            System.out.println("Waiter: Got the lock!");
        } finally {
            lock.unlock();
        }
    } else {
        System.out.println("Waiter: Timeout! Could not get lock in 2 seconds.");
    }
});
```

**What happens step by step:**

```
Time 0ms:     Holder starts, acquires lock, sleeps for 3 seconds

Time 500ms:   Waiter wakes up, calls tryLock(2, SECONDS)
              Lock is held by Holder
              Waiter starts WAITING (up to 2 seconds)

Time 2500ms:  2 seconds have passed. Holder still has the lock (it sleeps for 3 seconds)
              tryLock(2, SECONDS) returns FALSE -- TIMEOUT!
              Waiter prints "Timeout! Could not get lock in 2 seconds."

Time 3000ms:  Holder finally wakes up and calls unlock()
              (But Waiter already gave up 500ms ago)
```

**Output:**
```
Holder: Got lock, holding for 3 seconds...
Waiter: Trying to acquire lock with 2 second timeout...
Waiter: Timeout! Could not get lock in 2 seconds.
Holder: Released lock
```

**Key insight:** The timeline is critical. Holder holds for 3 seconds. Waiter starts waiting at 500ms with a 2-second timeout. 500ms + 2000ms = 2500ms. Holder does not release until 3000ms. So Waiter times out 500ms before the lock becomes available.

**Comparison:**

| Method | Behavior when lock is not available |
|--------|-------------------------------------|
| `lock.lock()` | Blocks forever until the lock is free |
| `lock.tryLock()` | Returns `false` immediately |
| `lock.tryLock(2, SECONDS)` | Waits up to 2 seconds, then returns `false` |

#### Demo 4: `conditionDemo()` -- Replacing wait/notify with Condition Variables

**The problem with `wait()`/`notify()`:**

With `synchronized` + `wait()`/`notify()`, you have ONE waiting queue per object. All waiters -- producers and consumers -- go into the same queue. When you call `notifyAll()`, ALL of them wake up, even though only producers or only consumers should be woken.

**The solution: `Condition` variables**

With `ReentrantLock`, you can create MULTIPLE conditions on the same lock. Producers wait on `notFull`. Consumers wait on `notEmpty`. You can wake them up independently.

**The BoundedBuffer class (inner class in ReentrantLockDemo):**

```java
static class BoundedBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();    // producers wait here
    private final Condition notEmpty = lock.newCondition();   // consumers wait here

    private final int[] items;
    private int count = 0;
    private int putIndex = 0;      // where to put the next item (circular)
    private int takeIndex = 0;     // where to take the next item (circular)

    public BoundedBuffer(int capacity) {
        items = new int[capacity];
    }
}
```

**The `put()` method -- called by the producer:**

```java
public void put(int item) throws InterruptedException {
    lock.lock();
    try {
        while (count == items.length) {
            System.out.println("Buffer full, producer waiting...");
            notFull.await();      // "I am a producer. Buffer is full. I will sleep on notFull."
        }                         // When woken, re-check (while loop) -- maybe another producer filled it again

        items[putIndex] = item;
        putIndex = (putIndex + 1) % items.length;    // circular: 0, 1, 2, 0, 1, 2, ...
        count++;

        notEmpty.signal();        // "I added an item. Wake up ONE consumer waiting on notEmpty."
    } finally {
        lock.unlock();
    }
}
```

**The `take()` method -- called by the consumer:**

```java
public int take() throws InterruptedException {
    lock.lock();
    try {
        while (count == 0) {
            System.out.println("Buffer empty, consumer waiting...");
            notEmpty.await();     // "I am a consumer. Buffer is empty. I will sleep on notEmpty."
        }

        int item = items[takeIndex];
        takeIndex = (takeIndex + 1) % items.length;
        count--;

        notFull.signal();         // "I removed an item. Wake up ONE producer waiting on notFull."
        return item;
    } finally {
        lock.unlock();
    }
}
```

**The demo runs a producer and consumer:**

```java
BoundedBuffer buffer = new BoundedBuffer(3);   // capacity = 3

// Producer thread: puts items 1, 2, 3, 4, 5
Thread producer = new Thread(() -> {
    for (int i = 1; i <= 5; i++) {
        buffer.put(i);
        System.out.println("Produced: " + i);
    }
});

// Consumer thread: takes 5 items
Thread consumer = new Thread(() -> {
    for (int i = 1; i <= 5; i++) {
        int item = buffer.take();
        System.out.println("Consumed: " + item);
    }
});
```

**What happens step by step (buffer capacity = 3):**

```
Time  Action                          Buffer State        count
----  ------                          ------------        -----
 1    Producer puts 1                 [1, _, _]           1
 2    Producer puts 2                 [1, 2, _]           2
 3    Producer puts 3                 [1, 2, 3]           3
 4    Producer tries to put 4
      count == 3 == capacity!
      Producer calls notFull.await()
      Producer RELEASES lock and SLEEPS on notFull
                                                          
 5    Consumer takes 1                [_, 2, 3]           2
      Consumer calls notFull.signal()
      Producer WAKES UP                                   
                                                          
 6    Producer re-acquires lock
      count is 2, not full anymore
      Producer puts 4                 [4, 2, 3]           3
      (circular: putIndex wrapped to 0)
      
 7    Producer tries to put 5
      count == 3 == capacity again!
      Producer sleeps on notFull again
      
 8    Consumer takes 2                [4, _, 3]           2
      Signals notFull -> producer wakes up
      
 9    Producer puts 5                 [4, 5, 3]           3
      Producer is DONE (produced all 5)
      
10    Consumer takes 3, 4, 5          [_, _, _]           0
      Consumer is DONE (consumed all 5)
```

**Why TWO Conditions instead of ONE?**

| | `synchronized` + `wait()/notifyAll()` | `ReentrantLock` + 2 Conditions |
|-|---------------------------------------|-------------------------------|
| Wait queues | ONE shared queue | TWO separate queues |
| `notifyAll()` wakes | ALL waiters (producers AND consumers) | Only the right group |
| Efficiency | Wastes CPU waking threads that cannot proceed | Precise -- only wakes the thread that can do work |
| Code clarity | Confusing -- who is waiting for what? | Clear -- producers wait on `notFull`, consumers on `notEmpty` |

**The circular buffer explained:**

```
putIndex and takeIndex wrap around using modulo:
  (index + 1) % capacity

For capacity = 3:
  0 -> 1 -> 2 -> 0 -> 1 -> 2 -> 0 -> ...

This means the array is reused forever without shifting elements.

Visual (capacity = 3):

  After put(1), put(2), put(3):     putIndex=0 (wrapped), takeIndex=0
    [1] [2] [3]
     ^takeIndex    ^putIndex(wrapped to 0)

  After take() returns 1:           putIndex=0, takeIndex=1
    [_] [2] [3]
         ^takeIndex

  After put(4):                     putIndex=1, takeIndex=1
    [4] [2] [3]
     ^putIndex  ^takeIndex
```

---

### 2.2 `ReadWriteLock` -- Many Readers, One Writer

**File:** `src/main/java/com/threadsdemo/advanced/ReadWriteLockDemo.java`

**The problem with `synchronized` and `ReentrantLock`:** They allow only ONE thread at a time -- period. If 100 threads want to READ a cache, they all block each other, even though reading is safe (nobody is changing the data). You are wasting time making threads wait for no reason.

```
With synchronized or ReentrantLock:

  Reader-1: lock -> read -> unlock
  Reader-2:                         lock -> read -> unlock     (waited for Reader-1!)
  Reader-3:                                                    lock -> read -> unlock
  
  Total: 3 x 50ms = 150ms  (all sequential -- SLOW)
```

**The solution:** `ReadWriteLock` has TWO locks inside it:

| Lock | Who can hold it | Rules |
|------|----------------|-------|
| **Read lock** | Multiple threads at the same time | Blocks only if a writer is active |
| **Write lock** | Only ONE thread at a time | Blocks ALL readers AND all other writers |

```
With ReadWriteLock:

  Reader-1: readLock -> read -> readUnlock
  Reader-2: readLock -> read -> readUnlock     (runs at the SAME TIME as Reader-1!)
  Reader-3: readLock -> read -> readUnlock     (runs at the SAME TIME!)
  
  Total: 50ms  (all parallel -- FAST)
```

#### The Lock Rules

```
                       Can a NEW reader acquire?    Can a NEW writer acquire?
                       ─────────────────────────    ─────────────────────────
Read lock is held      YES (readers share)          NO (writer must wait)
Write lock is held     NO (reader must wait)        NO (writer must wait)
No lock is held        YES                          YES
```

In one sentence: **Readers share. Writers are exclusive.**

#### The `Cache` Class -- Line by Line

The demo builds a thread-safe cache using `ReadWriteLock`. Here is every method explained:

```java
class Cache {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, Object> cache = new HashMap<>();   // NOT thread-safe on its own!
```

`HashMap` is not thread-safe. Without the lock, two threads calling `put()` simultaneously could corrupt the internal structure. The `ReadWriteLock` protects it.

**`get()` -- Read operation (multiple threads can do this at once):**

```java
public Object get(String key) {
    rwLock.readLock().lock();          // acquire the READ lock
    try {
        Thread.sleep(50);             // simulate slow read (e.g., disk or network)
        return cache.get(key);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return null;
    } finally {
        rwLock.readLock().unlock();    // ALWAYS release in finally
    }
}
```

If 3 readers call `get()` at the same time, all 3 acquire the read lock simultaneously -- no blocking. They all complete in ~50ms total (parallel), not 150ms (sequential).

**`put()` -- Write operation (only one thread, blocks everyone):**

```java
public void put(String key, Object value) {
    rwLock.writeLock().lock();         // acquire the WRITE lock (exclusive)
    try {
        Thread.sleep(200);            // simulate slow write (e.g., database update)
        cache.put(key, value);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        rwLock.writeLock().unlock();   // release -- readers can proceed now
    }
}
```

When a writer holds the write lock:
- All readers calling `get()` BLOCK at `readLock().lock()` until the writer releases
- Other writers calling `put()` BLOCK at `writeLock().lock()` until the writer releases

**`remove()` and `clear()` -- Also write operations:**

```java
public void remove(String key) {
    rwLock.writeLock().lock();         // modifies data -> needs write lock
    try {
        cache.remove(key);
    } finally {
        rwLock.writeLock().unlock();
    }
}

public void clear() {
    rwLock.writeLock().lock();         // modifies data -> needs write lock
    try {
        cache.clear();
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

Any method that **changes** the cache needs the write lock. Any method that only **reads** needs the read lock.

**`getAll()` -- Read operation that returns a copy:**

```java
public Map<String, Object> getAll() {
    rwLock.readLock().lock();          // only reads -> read lock is enough
    try {
        return new HashMap<>(cache);   // return a COPY, not the original
    } finally {
        rwLock.readLock().unlock();
    }
}
```

Why return a copy? If you returned the original `cache` reference, the caller could modify it outside the lock -- bypassing thread safety. A copy is safe to use without locks.

#### The `main()` Method -- What Happens at Runtime

```java
Cache cache = new Cache();

// Pre-populate (these run sequentially on the main thread, no concurrency yet)
cache.put("user:1", "Alice");      // writeLock, 200ms
cache.put("user:2", "Bob");        // writeLock, 200ms
cache.put("user:3", "Charlie");    // writeLock, 200ms
```

Then 4 threads are created:

```java
// 3 readers -- each reads 5 times with 100ms sleep between reads
Thread reader1 = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("Reader1: " + cache.get("user:1"));  // readLock, 50ms
        sleep(100);  // sleep 100ms between reads (NO lock held during sleep)
    }
}, "Reader-1");
// reader2 and reader3 are identical, reading user:2 and user:3

// 1 writer -- waits 200ms, then updates user:1
Thread writer = new Thread(() -> {
    sleep(200);  // let readers start first
    System.out.println("Writer: Updating user:1...");
    cache.put("user:1", "Alice Updated");  // writeLock, 200ms
    System.out.println("Writer: Update complete!");
}, "Writer");
```

All 4 threads are started and `join()`ed (main thread waits for all to finish).

#### Step-by-Step Timeline

```
Time     Reader-1              Reader-2              Reader-3              Writer
────     ────────              ────────              ────────              ──────
0ms      readLock()            readLock()            readLock()            sleeping(200ms)
         get("user:1")         get("user:2")         get("user:3")
         ALL THREE run in PARALLEL (read locks are shared)

50ms     -> "Alice"            -> "Bob"              -> "Charlie"
         readUnlock()          readUnlock()          readUnlock()
         sleep(100)            sleep(100)            sleep(100)

150ms    readLock()            readLock()            readLock()
         get (round 2)         get (round 2)         get (round 2)

200ms    (still reading)       (still reading)       (still reading)       wakes up
                                                                           writeLock()
                                                                           BLOCKED!
                                                                           (readers hold readLock)

200ms    readUnlock()          readUnlock()          readUnlock()
         sleep(100)            sleep(100)            sleep(100)
                                                                           
~200ms                                                                     writeLock acquired!
                                                                           put("user:1", "Alice Updated")
                                                                           (takes 200ms)

300ms    readLock()
         BLOCKED!  (writer holds writeLock)
         reader2 BLOCKED
         reader3 BLOCKED

~400ms                                                                     writeUnlock()
                                                                           Writer done!

~400ms   readLock acquired!    readLock acquired!    readLock acquired!
         -> "Alice Updated"    -> "Bob"              -> "Charlie"
         (reader1 now sees the updated value!)

...      rounds 4, 5 continue normally (all parallel reads)

~700ms   ALL DONE
         Final value: "Alice Updated"
```

**Key observations from the timeline:**

1. **Readers run in parallel** -- At time 0ms, all three readers hold the read lock simultaneously. This is the whole point of ReadWriteLock.

2. **Writer waits for readers** -- At time 200ms, the writer tries to acquire the write lock but readers still hold read locks. Writer must wait.

3. **Readers wait for writer** -- At time 300ms, readers try to acquire read locks but the writer holds the write lock. Readers must wait.

4. **Reader-1 sees the updated value** -- After the writer finishes, Reader-1's next `get("user:1")` returns `"Alice Updated"` instead of `"Alice"`. The write is visible to all subsequent reads.

#### Why Not Just Use `synchronized`?

| | `synchronized` | `ReentrantLock` | `ReadWriteLock` |
|-|---------------|-----------------|-----------------|
| Multiple readers at once? | No | No | **Yes** |
| Time for 3 parallel reads (50ms each) | 150ms (sequential) | 150ms (sequential) | **50ms (parallel)** |
| Best for | Simple mutual exclusion | Flexible locking (tryLock, timed) | **Read-heavy workloads** |

If your code has 95% reads and 5% writes (like a cache, config store, or lookup table), `ReadWriteLock` gives you a massive performance boost.

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

**Why two calls?** `shutdown()` says "no new tasks." But existing tasks keep running. `awaitTermination()` blocks the main thread until those tasks finish (or 5 seconds pass). If you skip `awaitTermination()`, your `main()` method may exit before the pool threads finish their work.

---

#### Demo 1: `fixedThreadPoolDemo()` -- Fixed Number of Threads

```java
ExecutorService executor = Executors.newFixedThreadPool(3);   // 3 threads, always

for (int i = 1; i <= 6; i++) {
    final int taskId = i;
    executor.submit(() -> {                                   // submit 6 tasks
        System.out.println("Task " + taskId + " running on " +
            Thread.currentThread().getName());
        Thread.sleep(500);                                    // each task takes 500ms
    });
}

executor.shutdown();
executor.awaitTermination(5, TimeUnit.SECONDS);
```

**What happens:**

The pool has 3 threads. You submit 6 tasks. The first 3 tasks start immediately. Tasks 4, 5, 6 wait in an internal queue until a thread becomes free.

```
Time       Thread-1        Thread-2        Thread-3        Queue
────       ────────        ────────        ────────        ─────
0ms        Task 1          Task 2          Task 3          [Task 4, Task 5, Task 6]
500ms      Task 1 done     Task 2 done     Task 3 done     
           picks Task 4    picks Task 5    picks Task 6    []  (queue empty)
1000ms     Task 4 done     Task 5 done     Task 6 done
           ALL COMPLETE
```

Total time: ~1000ms (2 batches of 3), not 3000ms (6 sequential).

**When to use:** You know the workload. Web server handling requests -- set N to the number of CPU cores.

---

#### Demo 2: `cachedThreadPoolDemo()` -- Create Threads on Demand

```java
ExecutorService executor = Executors.newCachedThreadPool();

for (int i = 1; i <= 5; i++) {
    final int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " on " +
            Thread.currentThread().getName());
    });
}
```

**How it works:**

| Property | Value |
|----------|-------|
| Core threads | 0 (no threads at start) |
| Max threads | Integer.MAX_VALUE (unlimited) |
| Idle timeout | 60 seconds (threads die after 60s of no work) |
| Queue | `SynchronousQueue` (no storage -- hand-off directly to a thread) |

When you submit Task 1, no threads exist. Pool creates Thread-1. If Task 2 arrives and Thread-1 is busy, pool creates Thread-2. If Task 3 arrives and Thread-1 just finished, Thread-1 is reused (no new thread created).

```
Task 1 arrives -> no free thread -> CREATE Thread-1 -> runs Task 1
Task 2 arrives -> Thread-1 busy  -> CREATE Thread-2 -> runs Task 2
Task 3 arrives -> Thread-1 free  -> REUSE Thread-1  -> runs Task 3
Task 4 arrives -> Thread-2 free  -> REUSE Thread-2  -> runs Task 4
Task 5 arrives -> Thread-1 free  -> REUSE Thread-1  -> runs Task 5
```

After 60 seconds of no tasks, idle threads are destroyed automatically.

**When to use:** Many short-lived tasks (e.g., handling quick API calls). **Danger:** If tasks are slow and you submit thousands, it creates thousands of threads and crashes with OutOfMemoryError.

---

#### Demo 3: `singleThreadExecutorDemo()` -- Sequential Execution

```java
ExecutorService executor = Executors.newSingleThreadExecutor();

for (int i = 1; i <= 3; i++) {
    final int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " on " +
            Thread.currentThread().getName() + " (always same thread)");
    });
}
```

**Output:**
```
Task 1 on pool-1-thread-1 (always same thread)
Task 2 on pool-1-thread-1 (always same thread)
Task 3 on pool-1-thread-1 (always same thread)
```

Only 1 thread ever exists. Tasks run one after another, in the order submitted. The queue stores tasks until the single thread is ready.

**When to use:** When tasks must execute in order (e.g., writing to a log file -- you do not want two threads writing simultaneously).

---

#### Demo 4: `scheduledThreadPoolDemo()` -- Delayed & Periodic Tasks

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// Run ONCE after a 1-second delay
scheduler.schedule(() -> {
    System.out.println("Delayed task executed!");
}, 1, TimeUnit.SECONDS);

// Run REPEATEDLY every 500ms, starting immediately
ScheduledFuture<?> periodicFuture = scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Periodic task at " + System.currentTimeMillis() % 10000);
}, 0, 500, TimeUnit.MILLISECONDS);
//  ^initial delay   ^period

// Let it run for 2 seconds, then cancel
Thread.sleep(2000);
periodicFuture.cancel(false);   // false = let current execution finish
```

**Timeline:**
```
Time       What happens
────       ────────────
0ms        Periodic task runs (initialDelay = 0)
500ms      Periodic task runs
1000ms     Periodic task runs + "Delayed task executed!" (1s delay done)
1500ms     Periodic task runs
2000ms     periodicFuture.cancel() -- stops repeating
```

| Method | What it does |
|--------|-------------|
| `schedule(task, 1, SECONDS)` | Run once, after 1 second |
| `scheduleAtFixedRate(task, 0, 500, MILLIS)` | Run immediately, then every 500ms |
| `scheduleWithFixedDelay(task, 0, 500, MILLIS)` | Run, wait 500ms after completion, run again |

**Difference between `AtFixedRate` and `WithFixedDelay`:**

```
scheduleAtFixedRate (every 500ms, task takes 200ms):
  |--200ms--|          |--200ms--|          |--200ms--|
  0        200   500  700   1000 1200       (fires every 500ms from START)

scheduleWithFixedDelay (500ms delay after completion, task takes 200ms):
  |--200ms--|---500ms---|--200ms--|---500ms---|
  0        200         700       900         1400    (500ms gap between END and next START)
```

---

#### Demo 5: `futureDemo()` -- Getting a Result Back

**The problem:** `executor.submit(Runnable)` runs a task but returns nothing. What if your task computes a value?

**The solution:** Submit a `Callable<T>` (like Runnable, but returns a value). You get back a `Future<T>`.

```java
ExecutorService executor = Executors.newSingleThreadExecutor();

// Callable<Integer> -- returns a value (unlike Runnable which returns void)
Future<Integer> future = executor.submit(() -> {
    System.out.println("Computing...");
    Thread.sleep(1000);      // simulate 1 second of work
    return 42;               // the result
});

System.out.println("Task submitted, doing other work...");
System.out.println("Is done? " + future.isDone());    // false (still computing)

Integer result = future.get();   // BLOCKS until the result is ready
System.out.println("Result: " + result);               // 42
System.out.println("Is done? " + future.isDone());     // true
```

**Timeline:**

```
Main Thread                          Pool Thread
───────────                          ───────────
submit(callable)  ──────────────>    starts computing...
"doing other work..."                Thread.sleep(1000)
isDone? false                        (still sleeping)
future.get() -- BLOCKS here          
  |                                  return 42
  +--- gets the value <────────────  done
"Result: 42"
isDone? true
```

**Key insight:** `future.get()` blocks the calling thread. This is the limitation that `CompletableFuture` (next section) solves.

| `Future` method | What it does |
|----------------|-------------|
| `get()` | Block forever until result is ready |
| `get(2, SECONDS)` | Block up to 2 seconds, then throw `TimeoutException` |
| `isDone()` | Check if task finished (non-blocking) |
| `cancel(true)` | Cancel the task (interrupt if running) |
| `isCancelled()` | Check if cancelled |

---

#### Demo 6: `invokeAllDemo()` -- Submit Many, Wait for All

```java
ExecutorService executor = Executors.newFixedThreadPool(3);

List<Callable<String>> tasks = new ArrayList<>();
for (int i = 1; i <= 5; i++) {
    final int taskId = i;
    tasks.add(() -> {
        Thread.sleep(taskId * 100);       // Task 1 = 100ms, Task 5 = 500ms
        return "Result-" + taskId;
    });
}

// Submit ALL at once -- blocks until ALL are done
List<Future<String>> futures = executor.invokeAll(tasks);

for (Future<String> f : futures) {
    System.out.println("  " + f.get());   // already done, get() returns immediately
}
```

**What happens:**

```
Pool has 3 threads, 5 tasks submitted:

Thread-1: Task 1 (100ms)  -> done -> Task 4 (400ms)  -> done
Thread-2: Task 2 (200ms)  -> done -> Task 5 (500ms)  -> done
Thread-3: Task 3 (300ms)  -> done

invokeAll() returns only when ALL 5 futures are complete.
Total time: ~700ms (Task 2 finishes at 200ms, picks up Task 5 at 200ms, finishes at 700ms)
```

**Output:**
```
All tasks completed:
  Result-1
  Result-2
  Result-3
  Result-4
  Result-5
```

Results are in the **same order** as the input list, regardless of which finished first.

**Comparison:**

| Method | What it does |
|--------|-------------|
| `submit(task)` | Submit one task, returns one `Future` |
| `invokeAll(list)` | Submit all tasks, **blocks** until all finish, returns list of `Future`s |
| `invokeAny(list)` | Submit all tasks, **blocks** until the FIRST one finishes, returns its result |

---

#### Demo 7: `customThreadPoolDemo()` -- Full Control with ThreadPoolExecutor

The `Executors.newFixedThreadPool()` and friends are convenience methods. Under the hood, they all create a `ThreadPoolExecutor`. This demo shows you how to configure it directly for full control:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                                     // corePoolSize
    4,                                     // maximumPoolSize
    60L, TimeUnit.SECONDS,                 // keepAliveTime
    new LinkedBlockingQueue<>(10),         // workQueue (bounded, max 10)
    new ThreadFactory() { ... },           // threadFactory
    new ThreadPoolExecutor.CallerRunsPolicy()  // rejectionHandler
);
```

**Every parameter explained:**

```
Parameter 1: corePoolSize = 2
  The pool always keeps at least 2 threads alive, even if they are idle.
  When you submit a task, if fewer than 2 threads exist, a new thread is created.

Parameter 2: maximumPoolSize = 4
  The pool can grow up to 4 threads. Extra threads (above core) are created
  ONLY when the queue is full and more tasks arrive.

Parameter 3: keepAliveTime = 60 seconds
  If the pool has more than 2 threads (the core size) and a thread has been
  idle for 60 seconds, it is killed. The pool shrinks back to 2.

Parameter 4: workQueue = LinkedBlockingQueue(10)
  When all core threads are busy, new tasks go into this queue.
  This queue holds max 10 tasks. If the queue is full AND pool is at max size,
  the rejection handler kicks in.

Parameter 5: threadFactory
  Controls HOW threads are created. Here we give custom names:
  "CustomThread-0", "CustomThread-1", etc.
  Also sets daemon=false (threads keep JVM alive until they finish).

Parameter 6: rejectionHandler = CallerRunsPolicy
  What happens when the queue is full AND all 4 threads are busy?
  CallerRunsPolicy: The submitting thread runs the task itself.
  (This provides back-pressure -- the submitter slows down.)
```

**How the pool grows and shrinks:**

```
Tasks    Pool State
─────    ──────────
Task 1   core < 2 -> CREATE Thread-1, run Task 1
Task 2   core < 2 -> CREATE Thread-2, run Task 2
Task 3   core = 2, both busy -> put in QUEUE [Task 3]
Task 4   core = 2, both busy -> put in QUEUE [Task 3, Task 4]
...
Task 12  core = 2, both busy -> QUEUE is FULL (10 items)
         pool < max(4) -> CREATE Thread-3, run Task 12
Task 13  QUEUE FULL, pool < max -> CREATE Thread-4, run Task 13
Task 14  QUEUE FULL, pool = max(4), all busy
         -> REJECTION HANDLER: CallerRunsPolicy
         -> The main thread runs Task 14 itself (back-pressure!)
```

**The 4 rejection policies:**

| Policy | What happens when queue is full AND pool is at max |
|--------|---------------------------------------------------|
| `AbortPolicy` (default) | Throws `RejectedExecutionException` -- task is lost |
| `CallerRunsPolicy` | The thread that called `submit()` runs the task itself -- slows down the producer |
| `DiscardPolicy` | Silently drops the task -- task is lost, no exception |
| `DiscardOldestPolicy` | Drops the oldest task in the queue, submits the new one |

In production, `CallerRunsPolicy` is the safest -- no data loss, natural back-pressure.

**The demo prints pool stats:**

```java
Thread.sleep(100);   // let tasks start
System.out.println("Active threads: " + executor.getActiveCount());   // how many are running tasks right now
System.out.println("Pool size: " + executor.getPoolSize());           // how many threads exist
System.out.println("Queue size: " + executor.getQueue().size());      // how many tasks waiting in queue
```

**Output:**
```
Core pool size: 2
Max pool size: 4
Task 1 on CustomThread-0
Task 2 on CustomThread-1
Active threads: 2
Pool size: 2
Queue size: 6
```

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

These two are the most confused classes in the entire Java concurrency API. You are right that both "wait for things to complete." But the **pattern** they solve is fundamentally different. Let me show you.

#### The Real Difference (Not Just "Reusable vs One-Shot")

The reusable thing is a side-effect. The **core** difference is:

**CountDownLatch: "I (one thread) am waiting for YOU ALL (many threads) to finish something."**
**CyclicBarrier: "WE ALL wait for EACH OTHER. Nobody moves until everyone arrives."**

```
CountDownLatch -- ONE waits for MANY:

  Main Thread:  "I am not starting until all 5 subsystems are ready"
                          |
                          | await()  (MAIN is the waiter)
                          |
                          v
  Subsystem-1: countDown()  ─┐
  Subsystem-2: countDown()  ─┤
  Subsystem-3: countDown()  ─┼──> count reaches 0 ──> Main thread unblocks
  Subsystem-4: countDown()  ─┤
  Subsystem-5: countDown()  ─┘

  Workers call countDown() and LEAVE. They do NOT wait.
  Only the main thread waits.


CyclicBarrier -- ALL wait for ALL:

  Worker-1:  "I finished my chunk. I will WAIT here for everyone else."
  Worker-2:  "I finished my chunk. I will WAIT here too."
  Worker-3:  "I finished my chunk. I will WAIT here too."
  Worker-4:  "I finished my chunk. I am the last one."
                          |
                          v
             ALL 4 unblock at the same time.
             Barrier resets. They can do it again for Phase 2.

  Every thread calls await() and WAITS.
  Nobody moves until ALL arrive.
```

#### The Restaurant Analogy

**CountDownLatch = Ordering food at a restaurant:**
- YOU (the customer) place 5 orders and wait.
- The kitchen prepares each dish independently. Each dish says "done" (`countDown()`).
- YOU wait until all 5 are done (`await()`).
- The cooks do NOT wait for each other. Dish 1 can finish and move on to the next order. Only YOU are waiting.
- Once all 5 are done, you eat. You cannot "re-order" -- the latch is used up.

**CyclicBarrier = A relay race with 4 runners:**
- All 4 runners are running Phase 1 independently.
- When Runner-1 finishes Phase 1, they WAIT at the handoff point.
- When Runner-2 finishes Phase 1, they ALSO WAIT.
- When Runner-3 finishes, they ALSO WAIT.
- When Runner-4 (the last one) arrives, ALL 4 move to Phase 2 simultaneously.
- This repeats for Phase 2, Phase 3, etc. (the barrier is reusable).

---

#### `CountDownLatch` -- The Code, Line by Line

**Method:** `countDownLatchDemo()` in `SynchronizationUtilitiesDemo.java`

```java
String[] subsystems = {"Database", "Kafka", "Redis-Cache", "Config-Server", "HealthCheck"};
CountDownLatch readyLatch = new CountDownLatch(subsystems.length);  // count = 5
```

A latch with count = 5. It will unblock when `countDown()` has been called 5 times.

```java
for (String subsystem : subsystems) {
    new Thread(() -> {
        int bootTimeMs = 300 + ThreadLocalRandom.current().nextInt(1200);
        System.out.printf("  [%s] booting ... (%d ms)%n", subsystem, bootTimeMs);
        Thread.sleep(bootTimeMs);          // simulate slow startup

        readyLatch.countDown();            // "I am ready!" (count: 5 -> 4 -> 3 ...)
        System.out.printf("  [%s] READY  (remaining: %d)%n",
                subsystem, readyLatch.getCount());

        // NOTE: This thread does NOT wait. It calls countDown() and is DONE.
        // It can go do other things. It is not stuck.

    }, subsystem).start();
}
```

Each subsystem thread boots (random time), calls `countDown()`, and **leaves**. It does NOT call `await()`. It does NOT wait for anyone.

```java
System.out.println("  Main thread waiting for all subsystems ...");
readyLatch.await();  // ONLY the main thread waits here
// This line blocks until count reaches 0 (all 5 called countDown)

System.out.println("  ALL SUBSYSTEMS READY — accepting traffic!");
```

**Only the main thread calls `await()`.** The 5 subsystem threads never wait. They fire-and-forget.

**Step-by-step timeline:**

```
Time    Main Thread              Database    Kafka     Redis     Config    Health    count
----    -----------              --------    -----     -----     ------    ------    -----
0ms     await() -- BLOCKED       booting     booting   booting   booting   booting   5

307ms                                        READY                                   4
                                             countDown()
                                             (Kafka is DONE. Goes away.)

550ms                                                            READY              3
                                                                 countDown()
                                                                 (Config is DONE.)

946ms                                                                      READY    2
                                                                           countDown()

1033ms                                                 READY                         1
                                                       countDown()

1050ms                            READY                                              0
                                  countDown()

1050ms  UNBLOCKED!                                                                   0
        "ALL READY!"
        (Main thread
         continues)
```

**Notice:**
- Kafka finished at 307ms and **left**. It did not wait for Database.
- Database was the slowest (1050ms). When it called `countDown()`, count hit 0.
- Main thread unblocked at 1050ms.
- The subsystem threads are INDEPENDENT. They do not know about each other.

**Can you reuse the latch?** NO. Once count = 0, calling `await()` again returns immediately. Calling `countDown()` again has no effect. It is done forever.

---

#### `CyclicBarrier` -- The Code, Line by Line

**Method:** `cyclicBarrierDemo()` in `SynchronizationUtilitiesDemo.java`

```java
final int CHUNK_COUNT = 4;
int[] partialCounts = new int[CHUNK_COUNT];  // shared results
AtomicInteger phaseNumber = new AtomicInteger(1);

CyclicBarrier barrier = new CyclicBarrier(CHUNK_COUNT, () -> {
    // This lambda runs ONCE when all 4 threads arrive at the barrier.
    // It runs on the LAST thread to arrive.
    int total = 0;
    for (int c : partialCounts) total += c;
    System.out.printf("  BARRIER -- Phase %d complete.  Aggregated: %,d%n",
            phaseNumber.getAndIncrement(), total);
});
```

A barrier with 4 parties. When all 4 call `await()`, the barrier action runs and all 4 unblock.

```java
for (int i = 0; i < CHUNK_COUNT; i++) {
    final int chunkId = i;

    workers[i] = new Thread(() -> {
        // ── Phase 1: Filter ──
        System.out.printf("  Chunk-%d  Phase 1 -- filtering%n", chunkId);
        Thread.sleep(200 + random);    // simulate work
        partialCounts[chunkId] = filtered;
        System.out.printf("  Chunk-%d  Phase 1 done%n", chunkId);

        barrier.await();   // "I am done with Phase 1. I will WAIT for everyone else."
                           // This thread is STUCK HERE until all 4 call await().

        // ── Phase 2: Transform ──
        // This line does NOT run until ALL 4 threads have called await() above.
        System.out.printf("  Chunk-%d  Phase 2 -- transforming%n", chunkId);
        Thread.sleep(150 + random);
        partialCounts[chunkId] = transformed;

        barrier.await();   // Wait for everyone AGAIN. Barrier resets automatically!

    }, "Chunk-" + chunkId);
    workers[i].start();
}
```

**Every thread calls `await()` and WAITS.** Nobody moves to Phase 2 until all 4 are done with Phase 1.

**Step-by-step timeline:**

```
Time    Chunk-0         Chunk-1         Chunk-2         Chunk-3         Barrier
----    -------         -------         -------         -------         -------
0ms     Phase 1 start   Phase 1 start   Phase 1 start   Phase 1 start   waiting for 4

350ms   Phase 1 done                                                     
        await()                                                          arrived: 1/4
        STUCK HERE                                                       

500ms                   Phase 1 done                                     
                        await()                                          arrived: 2/4
                        STUCK HERE                                       

600ms                                   Phase 1 done                     
                                        await()                          arrived: 3/4
                                        STUCK HERE                       

750ms                                                   Phase 1 done     
                                                        await()          arrived: 4/4
                                                                         ALL ARRIVED!

750ms   ─── BARRIER ACTION RUNS: "Phase 1 complete. Aggregated: 501,284" ───
        ─── BARRIER RESETS (back to 0/4) ───
        ─── ALL 4 THREADS UNBLOCK SIMULTANEOUSLY ───

750ms   Phase 2 start   Phase 2 start   Phase 2 start   Phase 2 start   waiting for 4

900ms   Phase 2 done
        await()                                                          arrived: 1/4
        STUCK HERE

1000ms                  Phase 2 done
                        await()                                          arrived: 2/4

1050ms                                  Phase 2 done
                                        await()                          arrived: 3/4

1100ms                                                  Phase 2 done
                                                        await()          arrived: 4/4

1100ms  ─── BARRIER ACTION RUNS: "Phase 2 complete. Aggregated: 499,102" ───
        ─── ALL 4 UNBLOCK ───
        ─── DONE ───
```

**Notice:**
- Chunk-0 finished Phase 1 at 350ms but it was **STUCK** until 750ms (waiting for Chunk-3).
- That is 400ms of waiting. In CountDownLatch, Chunk-0 would have moved on immediately.
- ALL 4 threads start Phase 2 at the exact same time (750ms).
- The barrier reset and worked again for Phase 2 without creating a new object.

---

#### Side-by-Side: The Same Scenario, Two Different Tools

Imagine 4 workers processing data. Here is how each tool would behave:

**With CountDownLatch:**
```
Worker-1: does Phase 1 ... countDown() ... immediately starts Phase 2
Worker-2: does Phase 1 ... countDown() ... immediately starts Phase 2
Worker-3: does Phase 1 ... countDown() ... immediately starts Phase 2
Worker-4: does Phase 1 ... countDown() ... immediately starts Phase 2

PROBLEM: Worker-1 might start Phase 2 while Worker-4 is still in Phase 1.
         Phase 2 might read Phase 1 results that are not ready yet.
         DATA CORRUPTION.
```

**With CyclicBarrier:**
```
Worker-1: does Phase 1 ... await() ... WAITS ...
Worker-2: does Phase 1 ... await() ... WAITS ...
Worker-3: does Phase 1 ... await() ... WAITS ...
Worker-4: does Phase 1 ... await() ... ALL UNBLOCK -> Phase 2 starts

SAFE: Nobody starts Phase 2 until ALL Phase 1 work is guaranteed complete.
```

---

#### The Complete Comparison

| | `CountDownLatch` | `CyclicBarrier` |
|-|-----------------|-----------------|
| **Core pattern** | ONE waits for MANY | ALL wait for ALL |
| **Who calls `await()`?** | Only the waiter (e.g., main thread) | **Every** participating thread |
| **Who calls `countDown()`?** | The workers | Nobody -- `await()` auto-counts |
| **Do workers wait?** | **NO** -- they countDown and leave | **YES** -- they await and BLOCK |
| **Reusable?** | No -- once count=0, it is dead | Yes -- resets automatically |
| **Barrier action?** | No | Yes -- a Runnable that runs when all arrive |
| **Number of threads** | Waiters and counters can be different | All parties must call `await()` |
| **Use case** | "Start after prerequisites are done" | "Sync threads between phases" |
| **Real-world** | App startup (wait for DB, Kafka, Cache) | Parallel processing (Phase 1 -> aggregate -> Phase 2) |

#### When You CANNOT Swap Them

**Use CountDownLatch when:**
- The "event sources" are not threads you control (e.g., async callbacks, external systems)
- The waiting thread is different from the counting threads
- You need exactly-once triggering (initialize something once)

**Use CyclicBarrier when:**
- The SAME threads need to synchronize at multiple points
- You have multi-phase processing (filter -> aggregate -> transform -> aggregate)
- Threads must NOT proceed until all are at the same point

---

### 4.3 `BlockingQueue` -- Producer-Consumer

**File:** `src/main/java/com/threadsdemo/advanced/BlockingQueueDemo.java`

#### What is the Producer-Consumer Pattern?

One of the most common patterns in backend systems. Two types of threads:
- **Producers** create work items (e.g., incoming HTTP requests, orders, events)
- **Consumers** process those work items (e.g., save to DB, send email, charge payment)

They are decoupled by a **queue** in between:

```
  [Producer-1] ──put──┐
                       ├──> [ QUEUE ] ──take──> [Consumer-1]
  [Producer-2] ──put──┘    (max 5)    ──take──> [Consumer-2]
                                       ──take──> [Consumer-3]
```

**Why not process inline?** During a traffic spike, 1000 orders arrive per second. If each order takes 500ms to process, your API would need 500 threads. With a queue, the API thread just drops the order in (fast) and returns immediately. 3 consumer threads process at their own pace.

#### Why `BlockingQueue` Instead of the Hand-Rolled Version?

In Part 1, you built a `BlockingQueue` manually with `synchronized`, `wait()`, and `notifyAll()`. That was 44 lines of code and easy to get wrong (spurious wakeups, forgetting to release locks, etc.).

`java.util.concurrent.LinkedBlockingQueue` does the same thing in zero lines of your code:

| | Hand-rolled (Part 1) | `LinkedBlockingQueue` |
|-|---------------------|----------------------|
| Thread safety | You write `synchronized` | Built-in |
| Blocking on full | You write `while + wait()` | `put()` blocks automatically |
| Blocking on empty | You write `while + wait()` | `take()` blocks automatically |
| Wake-up | You call `notifyAll()` | Handled internally |
| Bugs | Easy to make | Battle-tested, used in production |

#### The Setup -- Line by Line

```java
private static final int QUEUE_CAPACITY = 5;
private static final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
```

A bounded queue that holds max 5 orders. If a producer tries to `put()` a 6th order, it **blocks** until a consumer removes one. This is called **back-pressure** -- the producer is forced to slow down when the system is overloaded.

```java
private static final Order POISON_PILL = new Order(-1, "SHUTDOWN");
```

A special marker object. When a consumer takes this from the queue, it knows it should shut down. More on this later.

```java
record Order(int id, String item) {
    @Override
    public String toString() {
        return "Order{id=" + id + ", item='" + item + "'}";
    }
}
```

A Java `record` -- a compact class with just two fields (`id` and `item`). Records auto-generate `equals()`, `hashCode()`, and the constructor. The `toString()` override makes log output readable.

#### The Producers -- Line by Line

```java
int producerCount = 2;

for (int p = 0; p < producerCount; p++) {
    final int producerId = p + 1;       // 1 and 2

    producers[p] = new Thread(() -> {
        String[] items = {"Laptop", "Phone", "Tablet", "Monitor", "Keyboard"};

        for (int i = 0; i < items.length; i++) {
            Order order = new Order(producerId * 100 + i, items[i]);
            // Producer-1 creates: Order{100, Laptop}, Order{101, Phone}, ...
            // Producer-2 creates: Order{200, Laptop}, Order{201, Phone}, ...

            System.out.printf("  [Producer-%d]  putting %s  (queue size: %d)%n",
                    producerId, order, orderQueue.size());

            orderQueue.put(order);   // <-- THE KEY LINE
            // put() does one of two things:
            //   1. Queue has space -> adds the order and returns immediately
            //   2. Queue is FULL (5 items) -> BLOCKS here until a consumer takes one

            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
            // Sleep 100-300ms between orders (simulates real-world request rate)
        }

        System.out.printf("  [Producer-%d]  finished producing%n", producerId);
    }, "Producer-" + producerId);

    producers[p].start();
}
```

Each producer creates 5 orders (Laptop, Phone, Tablet, Monitor, Keyboard). With 2 producers, that is 10 orders total.

**What does `put()` do when the queue is full?**

```
Queue state:  [Order-100] [Order-200] [Order-101] [Order-201] [Order-102]
                                                                 ^ full (5/5)

Producer-1 calls orderQueue.put(Order-103)
  |
  +-> Queue is full!
  |
  +-> Producer-1 thread is BLOCKED (sleeping, not spinning)
  |
  +-> ... time passes ...
  |
  +-> Consumer-2 calls orderQueue.take() -> removes Order-100
  |
  +-> Queue has space! Producer-1 UNBLOCKS
  |
  +-> Order-103 is added to the queue
```

No `synchronized`, no `wait()`, no `notifyAll()`. The `LinkedBlockingQueue` does it all internally.

#### The Consumers -- Line by Line

**Why an Infinite Loop?**

A consumer does not know in advance how many orders it will process. Orders arrive unpredictably — sometimes 10 per second, sometimes none for 5 minutes. So the consumer must keep asking "is there anything for me?" forever, until someone tells it to stop.

```java
int consumerCount = 3;

for (int c = 0; c < consumerCount; c++) {
    final int consumerId = c + 1;       // 1, 2, and 3

    consumers[c] = new Thread(() -> {
```

We create 3 consumer threads. Each runs independently. They compete to `take()` from the same queue — whoever calls `take()` first gets the next order.

```java
        while (true) {                  // loop forever until we break out
```

This is the **event loop**. It runs one iteration per order. Each iteration does: take → check if poison → process → repeat.

```java
            Order order = orderQueue.take();
```

**This is the most important line in the entire consumer.** `take()` does one of two things:

```
Scenario A: Queue has items
  
  Queue:  [Order-100] [Order-101] [Order-102]
  
  Consumer-1 calls take()
    -> removes Order-100 from the front
    -> returns it IMMEDIATELY
    -> Consumer-1 now has order = Order-100
  
  Queue:  [Order-101] [Order-102]


Scenario B: Queue is EMPTY

  Queue:  []  (empty)
  
  Consumer-1 calls take()
    -> nothing to take
    -> Consumer-1 thread goes to SLEEP (not spinning, not burning CPU)
    -> Consumer-1 is STUCK on this line
    -> ... time passes ...
    -> Producer-2 calls queue.put(Order-201)
    -> Consumer-1 WAKES UP
    -> returns Order-201
    -> Consumer-1 now has order = Order-201
    
  The consumer spends ZERO CPU while waiting. It is asleep.
  This is why take() is better than polling in a loop:
  
  BAD:  while (queue.isEmpty()) { }          // burns 100% CPU doing nothing
  GOOD: Order order = queue.take();          // sleeps until there is something
```

```java
            if (order == POISON_PILL) {
                System.out.printf("  [Consumer-%d]  received shutdown signal%n", consumerId);
                break;                  // EXIT the while(true) loop
            }
```

Before processing, the consumer checks: "Is this a real order, or is this the shutdown signal?" If it is the poison pill, `break` exits the `while(true)` loop, the `run()` method ends, and the thread dies cleanly.

```java
            System.out.printf("  [Consumer-%d]  processing %s%n", consumerId, order);
            Thread.sleep(ThreadLocalRandom.current().nextInt(300, 700));
            System.out.printf("  [Consumer-%d]  completed  %s%n", consumerId, order);
```

Process the order (simulated with sleep). In a real system, this is where you would save to a database, charge a payment, send an email, etc.

```java
        }  // end while(true) -- loops back to take()
    }, "Consumer-" + consumerId);

    consumers[c].start();
}
```

After processing, the loop goes back to `take()`. If the queue is empty, the consumer sleeps. If there is another order, it processes it. This repeats forever — until a poison pill arrives.

**One iteration of a consumer's life, visualized:**

```
START OF LOOP ITERATION
     |
     v
  orderQueue.take()
     |
     +---> Queue empty? YES --> thread SLEEPS here (0% CPU)
     |                              |
     |                         producer puts() something
     |                              |
     |                         thread WAKES UP, gets the order
     |                              |
     +---> Queue has items? YES --> removes one, returns it
     |
     v
  Is it POISON_PILL?
     |
     +---> YES --> break (exit loop, thread dies)
     |
     +---> NO  --> process the order (300-700ms)
                       |
                       v
                  LOOP BACK TO take()
```

---

#### The Poison Pill Pattern -- Graceful Shutdown

**The problem:** Consumers run `while(true)`. How do you stop them?

**Approach 1: Set a boolean flag -- DOES NOT WORK**

```java
volatile boolean running = true;

// Consumer:
while (running) {              // checks flag each iteration
    Order order = queue.take();  // BUT -- consumer is STUCK HERE sleeping!
    process(order);              // never reaches the while check
}

// Main thread:
running = false;               // sets the flag, but consumer is asleep on take()
                               // consumer will NEVER wake up to check the flag
                               // DEADLOCK -- consumer is stuck forever
```

The consumer is blocked on `take()`. It will never reach the top of the `while` loop to check `running`. The flag is useless.

**Approach 2: Use `Thread.interrupt()` -- WORKS but ugly**

```java
consumerThread.interrupt();    // wakes up the thread blocked on take()
                               // take() throws InterruptedException
                               // consumer must catch it and exit

// Consumer:
try {
    Order order = queue.take();
} catch (InterruptedException e) {
    break;  // exit loop
}
```

This works, but you lose the order that was being processed. And you have to handle `InterruptedException` everywhere.

**Approach 3: POISON PILL -- The elegant solution**

Instead of fighting the queue, **work WITH the queue**. Send a special "shutdown" message THROUGH the queue. The consumer will naturally `take()` it and know to stop.

```java
// A special Order object that means "shut down"
private static final Order POISON_PILL = new Order(-1, "SHUTDOWN");
```

This is just a regular `Order` object, but we treat it as a signal. The consumer checks for it after every `take()`.

**The shutdown sequence -- step by step:**

```java
// Step 1: Wait for ALL producers to finish
for (Thread p : producers) p.join();
```

`join()` blocks the main thread until the producer thread completes. After this loop, all 10 orders (5 per producer) have been put into the queue. No more orders will ever arrive.

```java
System.out.println("All producers finished. Sending poison pills to consumers...");
```

At this point, the queue might still have unprocessed orders. Consumers are still running, processing them. We do NOT send poison pills yet until we are sure all real orders are in the queue.

```java
// Step 2: Send one poison pill PER consumer
for (int c = 0; c < consumerCount; c++) {   // consumerCount = 3
    orderQueue.put(POISON_PILL);              // puts 3 poison pills into the queue
}
```

**Why 3 poison pills?** Because there are 3 consumers. Each consumer `take()`s one item at a time from the queue. Each poison pill will be consumed by exactly ONE consumer.

Let me show you what would happen with different numbers:

```
With 3 POISON PILLs (CORRECT):

  Queue: [Order-204] [POISON] [POISON] [POISON]
  
  Consumer-1: take() -> Order-204 -> processes it
  Consumer-1: take() -> POISON_PILL -> break! (exits cleanly)
  Consumer-2: take() -> POISON_PILL -> break! (exits cleanly)
  Consumer-3: take() -> POISON_PILL -> break! (exits cleanly)
  
  Result: ALL 3 consumers exit. Program finishes.


With 1 POISON PILL (BUG):

  Queue: [Order-204] [POISON]
  
  Consumer-1: take() -> Order-204 -> processes it
  Consumer-2: take() -> POISON_PILL -> break! (exits cleanly)
  Consumer-1: take() -> ??? queue is empty -> BLOCKS FOREVER
  Consumer-3: take() -> ??? queue is empty -> BLOCKS FOREVER
  
  Result: 2 consumers are STUCK. Program hangs FOREVER. You have to kill -9 the process.


With 5 POISON PILLs (wastes 2, but still works):

  Queue: [POISON] [POISON] [POISON] [POISON] [POISON]
  
  Consumer-1: take() -> POISON_PILL -> break!
  Consumer-2: take() -> POISON_PILL -> break!
  Consumer-3: take() -> POISON_PILL -> break!
  
  Queue still has 2 unclaimed POISON_PILLs, but nobody cares -- all consumers are dead.
  Result: Works, but wasteful.
```

**Rule: Always send exactly N poison pills for N consumers.**

```java
// Step 3: Wait for all consumers to finish
for (Thread c : consumers) c.join();
```

After sending the poison pills, the main thread waits for all consumer threads to actually finish. This ensures all orders are fully processed (not just taken from the queue, but completed).

```java
System.out.println("All orders processed. Queue is empty: " + orderQueue.isEmpty());
// Output: All orders processed. Queue is empty: true
```

**The complete shutdown flow:**

```
STEP 1: Main thread waits for producers to finish
        ─────────────────────────────────────────

  Main:      producers[0].join()  --> waits for Producer-1 to finish all 5 orders
             producers[1].join()  --> waits for Producer-2 to finish all 5 orders
  
  At this point: 10 orders have been put(). Producers are done.
                 Queue may still have some orders being processed by consumers.


STEP 2: Main thread sends poison pills
        ────────────────────────────────

  Main:      orderQueue.put(POISON_PILL)   --> 1st pill enters queue
             orderQueue.put(POISON_PILL)   --> 2nd pill enters queue
             orderQueue.put(POISON_PILL)   --> 3rd pill enters queue

  Queue now: [...remaining orders...] [POISON] [POISON] [POISON]
  
  The pills go to the END of the queue. This is important!
  It means consumers will process ALL remaining real orders BEFORE hitting a pill.
  No orders are lost.


STEP 3: Consumers naturally consume remaining orders, then hit the pills
        ──────────────────────────────────────────────────────────────────

  Consumer-1: take() -> Order-203 -> processes it -> take() -> POISON -> break!
  Consumer-2: take() -> Order-204 -> processes it -> take() -> POISON -> break!
  Consumer-3: take() -> POISON -> break!  (was idle, no orders left for it)


STEP 4: Main thread confirms all consumers are done
        ────────────────────────────────────────────

  Main:      consumers[0].join()  --> Consumer-1 has exited
             consumers[1].join()  --> Consumer-2 has exited
             consumers[2].join()  --> Consumer-3 has exited
  
  Main:      "All orders processed. Queue is empty: true"
  
  Program exits cleanly. No threads stuck. No orders lost.
```

**Why the pills go to the END of the queue:**

```
Queue at the moment we send pills:

  FRONT                                              BACK
  [Order-103] [Order-204] [Order-104]   <-- real orders still waiting
                                         [POISON] [POISON] [POISON]  <-- pills added at the end

  Consumers take from the FRONT. So they process:
    Order-103 first
    Order-204 second
    Order-104 third
    POISON fourth   --> Consumer exits

  EVERY real order is processed before any consumer shuts down.
  This is why the poison pill pattern guarantees NO DATA LOSS.
```

**Why `order == POISON_PILL` (reference equality)?**

```java
if (order == POISON_PILL) {   // == compares object REFERENCE, not content
```

This uses `==` (same object in memory), not `.equals()` (same content):

```
POISON_PILL is created ONCE:
  private static final Order POISON_PILL = new Order(-1, "SHUTDOWN");
  This object lives at memory address 0x7F3A (for example).

When the consumer takes it from the queue:
  Order order = queue.take();   // order now points to address 0x7F3A
  
  order == POISON_PILL          // is 0x7F3A == 0x7F3A? YES. Same object.

Why not use .equals()?
  A real order could theoretically have id=-1 and item="SHUTDOWN".
  That would have a DIFFERENT memory address (e.g., 0x8B2C).
  
  realOrder == POISON_PILL      // 0x8B2C == 0x7F3A? NO. Different objects. SAFE.
  realOrder.equals(POISON_PILL) // id matches, item matches? YES. FALSE POSITIVE!
  
  Using == prevents false positives. Only THE EXACT poison pill object triggers shutdown.
```

#### The Complete Timeline

```
Time     Producer-1          Producer-2          Queue (max 5)          Consumer-1       Consumer-2       Consumer-3
────     ──────────          ──────────          ─────────────          ──────────       ──────────       ──────────
0ms      put(100,Laptop)     put(200,Laptop)     [100-Lap, 200-Lap]    take->100-Lap    take->200-Lap    (empty,blocked)
                                                                        processing...    processing...

200ms    put(101,Phone)      put(201,Phone)      [101-Ph, 201-Ph]                                        take->101-Ph
                                                                                                          processing...

400ms    put(102,Tablet)     put(202,Tablet)     [201-Ph, 102-Tab,     done 100-Lap
                                                  202-Tab]             take->201-Ph

600ms    put(103,Monitor)    put(203,Monitor)     [102-Tab, 202-Tab,                    done 200-Lap
                                                   103-Mon, 203-Mon]                    take->102-Tab
                                                   (4/5)

800ms    put(104,Keyboard)   put(204,Keyboard)    [202-Tab, 103-Mon,                                     done 101-Ph
                                                   203-Mon, 104-Key,                                     take->202-Tab
                                                   204-Key]  FULL!

         Producer-1 DONE    Producer-2 tries
                            put() -> BLOCKED!
                            (queue is FULL)

~900ms                                                                  done 201-Ph
                                                                        take->103-Mon
                                                   SPACE! Producer-2
                                                   unblocks, puts 204

~1200ms  DONE               DONE                  Producers join()
                                                   Main sends 3 POISON_PILLs

                                                   [POISON, POISON,     take->POISON     take->POISON     take->POISON
                                                    POISON]             break!           break!           break!

         ALL CONSUMERS EXIT CLEANLY.
         Queue is empty: true
```

#### `put()` vs `offer()` vs `add()` -- Which to Use?

| Method | When queue is full | Return type |
|--------|-------------------|-------------|
| `put(item)` | **BLOCKS** until space is available | void |
| `offer(item)` | Returns `false` immediately | boolean |
| `offer(item, 2, SECONDS)` | Waits up to 2 seconds, then returns `false` | boolean |
| `add(item)` | Throws `IllegalStateException` | boolean |

| Method | When queue is empty | Return type |
|--------|-------------------|-------------|
| `take()` | **BLOCKS** until an item is available | item |
| `poll()` | Returns `null` immediately | item or null |
| `poll(2, SECONDS)` | Waits up to 2 seconds, then returns `null` | item or null |
| `peek()` | Returns head without removing (null if empty) | item or null |

In this demo, we use `put()`/`take()` because we WANT blocking -- that is the whole point of back-pressure.

#### Why This Pattern Matters in Real Systems

```
Without BlockingQueue (inline processing):

  API Thread receives order -> processes order (500ms) -> returns response
  
  Problem: During a spike of 1000 orders/second, you need 500 threads.
           Server crashes with OutOfMemoryError.

With BlockingQueue:

  API Thread receives order -> queue.put(order) (instant) -> returns response
  3 Consumer threads process at their own pace (500ms each)
  
  Result: API stays fast. Consumers process at ~6 orders/second.
          Queue absorbs the burst. No crash.
          If queue fills up, put() blocks -> API slows down naturally (back-pressure).
```

This is the foundation of every high-throughput system: Kafka, RabbitMQ, SQS -- they are all variations of this producer-consumer-queue pattern.

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
