# Java Multithreading Complete Guide

> **A comprehensive guide to Java concurrency and multithreading — from basics to advanced concepts**

Master Java threading from the ground up — covering thread creation, synchronization, thread states, deadlocks, and inter-thread communication with real working code.

---

## 📋 Table of Contents

1. [Multitasking Overview](#1-multitasking-overview)
2. [What is a Thread?](#2-what-is-a-thread)
3. [The Main Thread](#3-the-main-thread)
4. [Creating Threads - Two Ways](#4-creating-threads---two-ways)
5. [Daemon Threads](#5-daemon-threads)
6. [Thread Lifecycle & States](#6-thread-lifecycle--states)
7. [Thread Priority](#7-thread-priority)
8. [Race Condition - The Problem](#8-race-condition---the-problem)
9. [Synchronization - The Solution](#9-synchronization---the-solution)
10. [Synchronized Blocks](#10-synchronized-blocks)
11. [Static Synchronized Methods](#11-static-synchronized-methods)
12. [Volatile Keyword](#12-volatile-keyword)
13. [Thread Communication (wait/notify)](#13-thread-communication-waitnotify)
14. [Thread Joining](#14-thread-joining)
15. [Deadlocks](#15-deadlocks)
16. [ReentrantLock](#16-reentrantlock)
17. [ReadWriteLock](#17-readwritelock)
18. [Atomic Classes](#18-atomic-classes)
19. [ExecutorService & Thread Pools](#19-executorservice--thread-pools)
20. [CompletableFuture](#20-completablefuture)
21. [Synchronization Utilities](#21-synchronization-utilities)
22. [Virtual Threads (Java 21+)](#22-virtual-threads-java-21)
23. [Best Practices](#23-best-practices)

---

## 1. Multitasking Overview

**Multitasking** allows several activities to occur concurrently on the computer. There are two types:

### Process-Based Multitasking
Allows **processes (programs)** to run concurrently on the computer.

> **Example:** Running MS Paint while also working with Word Processor.

### Thread-Based Multitasking
Allows **parts of the same program** to run concurrently.

> **Example:** MS Word printing and formatting text at the same time.

### Threads vs Processes

| Aspect | Threads | Processes |
|--------|---------|-----------|
| **Address Space** | Share same address space | Separate address spaces |
| **Context Switching** | Less expensive | More expensive |
| **Communication** | Low cost (shared memory) | High cost (IPC needed) |
| **Resource Sharing** | Easy | Complex |

### Why Multithreading?

```
┌─────────────────────────────────────────────────────────────────┐
│                    SINGLE-THREADED                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Task 1 ████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│   Waiting         ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│   Task 2          ░░░░░░░░░░░░████████████░░░░░░░░░░░░░░░░░░░░  │
│                                                                  │
│   ⚠️ CPU cycles WASTED while waiting!                           │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                    MULTI-THREADED                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Thread 1 ████████████░░░░████████████░░░░████████████         │
│   Thread 2 ░░░░████████████░░░░████████████░░░░████████████     │
│                                                                  │
│   ✅ CPU utilized efficiently!                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. What is a Thread?

A **thread** is an independent sequential path of execution within a program.

### Three Important Concepts in Java Multithreading:

1. **Creating threads** and providing the code that gets executed
2. **Accessing common data** through synchronization
3. **Transitioning between thread states**

```
┌─────────────────────────────────────────────────────────────────┐
│                         PROCESS                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────┐    ┌──────────┐    ┌──────────┐                 │
│   │ Thread 1 │    │ Thread 2 │    │ Thread 3 │                 │
│   │  (Main)  │    │ (Worker) │    │ (Worker) │                 │
│   └──────────┘    └──────────┘    └──────────┘                 │
│         │              │               │                        │
│         └──────────────┴───────────────┘                        │
│                        │                                         │
│                 Shared Memory (Heap)                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. The Main Thread

When a standalone application runs, a **user thread is automatically created** to execute the `main()` method. This thread is called the **main thread**.

```java
public static void main(String[] args) {
    // This code runs in the "main" thread
    System.out.println("Current thread: " + Thread.currentThread().getName());
    // Output: Current thread: main
}
```

### Key Points:

- If **no other user threads** are spawned, the program terminates when `main()` finishes
- All other threads, called **child threads**, are spawned from the main thread
- The `main()` method can finish, but the program keeps running until **all user threads complete**
- The JVM distinguishes between **user threads** and **daemon threads**
- As long as a **user thread is alive**, the JVM does not terminate

---

## 4. Creating Threads - Two Ways

A thread in Java is represented by an object of the `Thread` class. Creating threads is achieved in one of two ways:

### Method 1: Extending Thread Class

```java
// Thread1.java
public class Thread1 extends Thread {

    public Thread1(String threadName) {
        super(threadName);  // Set thread name via parent constructor
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Inside the thread " + Thread.currentThread().getName() 
                             + " with value of i as " + i);
        }
    }
}
```

**Usage:**
```java
// Create and start the thread
Thread thread1 = new Thread1("MyThread");
thread1.start();  // Calls run() in a new thread

// ⚠️ DON'T call run() directly - it won't create a new thread!
// thread1.run();  // WRONG - runs in main thread
```

**Output:**
```
Inside the thread MyThread with value of i as 0
Inside the thread MyThread with value of i as 1
Inside the thread MyThread with value of i as 2
Inside the thread MyThread with value of i as 3
Inside the thread MyThread with value of i as 4
```

---

### Method 2: Implementing Runnable Interface (PREFERRED ✅)

```java
// Thread2.java
public class Thread2 implements Runnable {

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Inside the thread " + Thread.currentThread().getName() 
                             + " with value of i as " + i);
        }
    }
}
```

**Usage:**
```java
// Way 1: Create Runnable and pass to Thread
Thread2 runnable = new Thread2();
Thread thread = new Thread(runnable, "RunnableThread");
thread.start();

// Way 2: Lambda expression (Modern & Clean)
Thread thread2 = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("Inside the thread " + Thread.currentThread().getName() 
                         + " with value of i as " + i);
    }
}, "LambdaThread");
thread2.start();
```

---

### Comparison: Thread vs Runnable

| Aspect | Extending Thread | Implementing Runnable |
|--------|------------------|----------------------|
| **Inheritance** | Can't extend another class | Can extend other classes |
| **Flexibility** | Low (single inheritance) | High |
| **Reusability** | Low | High |
| **Resource Sharing** | Complex | Easy |
| **Best Practice** | ❌ Avoid | ✅ Preferred |

**Why Runnable is preferred:**
1. Java doesn't support multiple inheritance
2. Separates the task (what to do) from the thread (how to run)
3. Same Runnable can be passed to multiple threads
4. Works with ExecutorService and thread pools

---

## 5. Daemon Threads

A **daemon thread** is a background thread that doesn't prevent JVM from exiting.

```java
Thread thread1 = new Thread1("threadThread");
thread1.setDaemon(true);  // Must set BEFORE start()
thread1.start();
```

### Key Points:

| Aspect | User Thread | Daemon Thread |
|--------|-------------|---------------|
| **JVM Termination** | JVM waits for completion | JVM doesn't wait |
| **Purpose** | Main application logic | Background tasks (GC, monitoring) |
| **Example** | Main thread | Garbage Collector |

```
┌─────────────────────────────────────────────────────────────────┐
│                    DAEMON vs USER THREADS                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   User Thread (main)     ████████████████████                   │
│   User Thread (worker)   ░░░░████████████████████████           │
│   Daemon Thread          ░░░░░░░░░░░░████████████░░░░░░░░░░░░░  │
│                                      │                          │
│                                      │                          │
│                          JVM EXIT ───┘                          │
│                          (when all user threads finish)         │
│                                                                  │
│   ⚠️ Daemon thread is KILLED when user threads complete!       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Important:** `setDaemon(true)` must be called **before** `start()`, otherwise `IllegalThreadStateException` is thrown.

---

## 6. Thread Lifecycle & States

A thread goes through several states during its lifetime:

```
                         ┌────────────────────────────────────────────────────────┐
                         │                                                        │
    ┌─────┐   start()    │  ┌──────────┐                                         │
    │ NEW │─────────────►│  │ RUNNABLE │◄────────────────────────────────────────┤
    └─────┘              │  └────┬─────┘                                         │
                         │       │                                                │
                         │       ├────────────────┬────────────────┐              │
                         │       │                │                │              │
                         │       ▼                ▼                ▼              │
                         │ ┌──────────┐    ┌──────────┐    ┌────────────┐        │
                         │ │ BLOCKED  │    │ WAITING  │    │   TIMED    │        │
                         │ │(waiting  │    │(wait(),  │    │  WAITING   │        │
                         │ │for lock) │    │ join())  │    │ (sleep())  │        │
                         │ └────┬─────┘    └────┬─────┘    └─────┬──────┘        │
                         │      │               │                │               │
                         │      └───────────────┴────────────────┘               │
                         │                      │                                 │
                         │                      │ Lock acquired /                │
                         │                      │ notify() / timeout             │
                         │                      │                                 │
                         └──────────────────────┴─────────────────────────────────┘
                            
    RUNNABLE ──────────────────────────────────────────────────────────────────────►  TERMINATED
                               run() completes or exception
```

### Thread State Constants

| State | Constant | Description |
|-------|----------|-------------|
| **New** | `NEW` | Created but not started |
| **Runnable** | `RUNNABLE` | Executing in the JVM |
| **Blocked** | `BLOCKED` | Blocked while waiting for lock |
| **Waiting** | `WAITING` | Waiting indefinitely for another thread |
| **Timed Waiting** | `TIMED_WAITING` | Waiting for a specified time |
| **Terminated** | `TERMINATED` | Completed execution |

### Code Example: Monitoring Thread States

```java
Thread thread3 = new Thread(() -> {
    try {
        Thread.sleep(1);  // TIMED_WAITING
        for (int i = 10000; i > 0; i--);  // RUNNABLE
    } catch (InterruptedException e) {
        System.out.println("Thread was interrupted");
    }
}, "States");

thread3.start();

// Monitor thread state
while (true) {
    Thread.State state = thread3.getState();
    System.out.println(state);
    if (state == Thread.State.TERMINATED) {
        break;
    }
}
```

**Output:**
```
RUNNABLE
TIMED_WAITING
TIMED_WAITING
RUNNABLE
RUNNABLE
TERMINATED
```

---

### Running, Sleeping, Waiting Transitions

```
┌─────────────────────────────────────────────────────────────────┐
│                    THREAD STATE TRANSITIONS                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────┐     yield()     ┌──────────────┐            │
│   │   RUNNING    │◄───────────────►│ READY TO RUN │            │
│   └──────┬───────┘   scheduling    └──────────────┘            │
│          │                                ▲                     │
│          │ sleep(ms)                      │                     │
│          ▼                                │ Time elapsed /      │
│   ┌──────────────┐                        │ Interrupted        │
│   │   SLEEPING   │────────────────────────┘                     │
│   └──────────────┘                                              │
│                                                                  │
│          │ wait()                                               │
│          ▼                                                      │
│   ┌──────────────┐    notify() /    ┌──────────────────┐       │
│   │   WAITING    │─────────────────►│ BLOCKED FOR LOCK │       │
│   └──────────────┘    notifyAll()   └────────┬─────────┘       │
│                                              │                  │
│                                              │ Lock acquired    │
│                                              ▼                  │
│                                       ┌──────────────┐         │
│                                       │ READY TO RUN │         │
│                                       └──────────────┘         │
│                                                                  │
│   ⚠️ NOTE: Thread in SLEEPING state still HOLDS the lock!      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Key Points:**
- `sleep()` does **NOT release the lock** - thread holds it while sleeping
- `wait()` **releases the lock** - another thread can acquire it
- After `notify()`, thread goes to **BLOCKED** state first, not directly to RUNNING

---

## 7. Thread Priority

Priorities are integer values from **1 (lowest)** to **10 (highest)**. Default is **5**.

| Constant | Value |
|----------|-------|
| `Thread.MIN_PRIORITY` | 1 |
| `Thread.NORM_PRIORITY` | 5 (default) |
| `Thread.MAX_PRIORITY` | 10 |

```java
Thread thread = new Thread(() -> {
    System.out.println(Thread.currentThread());
}, "Our Thread");

thread.start();
thread.join();

System.out.println("Thread priority: " + thread.getPriority());  // 5
```

### Key Points:

- A thread **inherits** the priority of its parent thread
- `setPriority()` is **advisory** — JVM is not obliged to honor it
- Thread scheduling depends on JVM implementation:
  - **Preemptive Scheduling**: Higher priority thread can preempt lower priority
  - **Time-Sliced (Round-Robin)**: Each thread gets a time slice

---

## 8. Race Condition - The Problem

A **race condition** occurs when two or more threads simultaneously update the same value and leave it in an **undefined or inconsistent state**.

### The Problem Demonstrated

```java
// ❌ UNSAFE - Race Condition!
public class UnsafeStack {
    private int[] array;
    private int top = -1;

    public boolean push(int element) {
        if (isFull()) return false;
        
        ++top;                    // Step 1: Increment top
        // 💥 Another thread might read 'top' here!
        array[top] = element;     // Step 2: Set value
        return true;
    }

    public int pop() {
        if (isEmpty()) return -1;
        
        int obj = array[top];     // Step 1: Read value
        // 💥 Another thread might modify 'top' here!
        --top;                    // Step 2: Decrement top
        return obj;
    }
}
```

**What can go wrong:**

```
Thread A (push)              Thread B (pop)              top    array[0]
─────────────────           ─────────────────           ────   ─────────
                                                         -1     empty
++top (top = 0)                                           0     empty
                            obj = array[0] = empty        0     empty
                            --top                        -1     empty
array[0] = 100                                           -1     100

Result: Value 100 is LOST! ❌
```

---

## 9. Synchronization - The Solution

**Threads share the same memory space** — they can share resources (objects). However, there are critical situations where only **one thread at a time** should access a shared resource.

### Synchronized Methods

```java
public synchronized boolean push(int element) {
    // Only ONE thread can execute this at a time
    // Implicitly locks on 'this'
}

public synchronized int pop() {
    // Same lock as push - 'this'
}
```

### Rules of Synchronization:

1. A thread must **acquire the object lock** before entering a shared resource
2. No other thread can enter if another thread **holds the lock**
3. If a thread cannot acquire the lock, it is **blocked** (must wait)
4. When a thread exits, the **lock is relinquished**
5. **No assumptions** should be made about the order threads acquire locks

### Thread-Safe Stack Implementation

```java
// Stack.java - Thread-Safe with synchronized
public class Stack {

    private int[] array;
    private int top;
    private int capacity;

    public Stack(int capacity) {
        array = new int[capacity];
        top = -1;
    }

    public boolean isFull() {
        return top == array.length - 1;
    }

    public boolean isEmpty() {
        return top == -1;
    }

    // ✅ THREAD-SAFE push
    public boolean push(int element) {
        synchronized (this) {
            if (isFull()) {
                return false;
            }

            ++top;
            try { 
                Thread.sleep(1000);
            } catch (Exception e) {}
            array[top] = element;
            return true;
        }
    }

    // ✅ THREAD-SAFE pop
    public int pop() {
        synchronized (this) {
            if (isEmpty()) {
                return Integer.MIN_VALUE;
            }

            int obj = array[top];
            array[top] = Integer.MIN_VALUE;

            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
            
            --top;
            return obj;
        }
    }
}
```

### How synchronized Works

```
┌─────────────────────────────────────────────────────────────────┐
│                    SYNCHRONIZED BLOCK                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Thread A                         Thread B                      │
│      │                                │                          │
│      │ synchronized(this) {           │                          │
│      │     // Acquires lock           │                          │
│      │     🔒 LOCKED                  │ synchronized(this) {     │
│      │     ++top;                     │     // Waiting for lock  │
│      │     array[top] = 100;          │     ⏳ BLOCKED           │
│      │     // Releases lock           │     ⏳ BLOCKED           │
│      │ }                              │ }                        │
│      │                                │     🔒 Now acquires lock │
│      │                                │     --top;               │
│      │                                │     return obj;          │
│      │                                │ }                        │
│                                                                  │
│   Result: Operations are ATOMIC - no interleaving! ✅            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. Synchronized Blocks

While synchronized methods lock on `this`, **synchronized blocks** allow execution to be synchronized on **any arbitrary object**.

```java
synchronized (objectRefExpression) {
    // code block
}
```

### 🔑 Important: Same Lock = Mutual Exclusion Across ALL Methods

When multiple methods are synchronized on the **same lock**, only **ONE thread can execute ANY of those methods** at a time.

```java
public class Stack {
    
    // Both methods use synchronized(this) - SAME LOCK!
    
    public boolean push(int element) {
        synchronized (this) {  // Lock on 'this'
            // ...
        }
    }

    public int pop() {
        synchronized (this) {  // Same lock - 'this'
            // ...
        }
    }
}
```

**What this means:**

```
┌─────────────────────────────────────────────────────────────────┐
│           SAME LOCK = MUTUALLY EXCLUSIVE METHODS                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Thread A calls push()        Thread B calls pop()             │
│          │                            │                          │
│          ▼                            ▼                          │
│   synchronized(this) {         synchronized(this) {             │
│       🔒 Acquires lock             ⏳ BLOCKED!                   │
│       // doing push                 // Can't enter pop()        │
│       // ...                        // even though it's a       │
│       // ...                        // different method!        │
│   } // Releases lock                                            │
│          │                            │                          │
│          │                            ▼                          │
│          │                     🔒 Now acquires lock              │
│          │                     // doing pop                      │
│          │                     }                                 │
│                                                                  │
│   ⚠️ Thread B CANNOT call pop() while Thread A is in push()    │
│      because BOTH methods lock on the SAME object (this)        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Using Different Locks (Use with Caution!)

```java
public class Example {
    private final Object pushLock = new Object();
    private final Object popLock = new Object();
    
    public void push() {
        synchronized (pushLock) {  // Different lock
            // ...
        }
    }
    
    public void pop() {
        synchronized (popLock) {  // Different lock - CAN run concurrently!
            // ...
        }
    }
}
```

⚠️ **Warning:** Using different locks for related operations can lead to race conditions!

---

## 11. Static Synchronized Methods

A thread acquiring the **class lock** (for static synchronized methods) has **no effect** on threads acquiring **object locks** (for instance synchronized methods).

```java
public class Example {
    
    // Uses CLASS lock (Example.class)
    public static synchronized void staticMethod() {
        // ...
    }
    
    // Uses OBJECT lock (this)
    public synchronized void instanceMethod() {
        // ...
    }
}
```

```
┌─────────────────────────────────────────────────────────────────┐
│         STATIC vs INSTANCE SYNCHRONIZED METHODS                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Static Method                    Instance Method               │
│   ─────────────                    ───────────────               │
│   Lock: Example.class              Lock: this (object)           │
│                                                                  │
│   Thread A                         Thread B                      │
│      │                                │                          │
│      │ staticMethod() {               │ instanceMethod() {       │
│      │     🔒 Class lock             │     🔒 Object lock       │
│      │     // working                 │     // working           │
│      │ }                              │ }                        │
│                                                                  │
│   ✅ Both can run CONCURRENTLY - different locks!               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Key Points:**
- Static methods use `.class` as lock
- Instance methods use `this` as lock
- They are **independent** - can run concurrently
- A subclass decides whether inherited synchronized method remains synchronized

---

## 12. Volatile Keyword

The **volatile** keyword is used for variables that may be accessed by multiple threads. It ensures **visibility** of changes across threads.

```java
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

### Key Points:

| Aspect | volatile | synchronized |
|--------|----------|--------------|
| **Visibility** | ✅ Yes | ✅ Yes |
| **Atomicity** | ❌ No | ✅ Yes |
| **Blocking** | ❌ No | ✅ Yes |
| **Use Case** | Flags, singleton | Critical sections |

---

## 13. Thread Communication (wait/notify)

### Important Methods

```java
final void wait() throws InterruptedException
final void wait(long timeout) throws InterruptedException
final void wait(long timeout, int nanos) throws InterruptedException
final void notify()
final void notifyAll()
```

### How wait/notify Works

```
┌─────────────────────────────────────────────────────────────────┐
│                    WAIT / NOTIFY MECHANISM                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Thread A                         Thread B                      │
│      │                                │                          │
│      │ synchronized(lock) {           │                          │
│      │     while (!condition) {       │                          │
│      │         lock.wait();           │ synchronized(lock) {     │
│      │         // Releases lock! ─────►      🔒 Acquires lock   │
│      │         // Goes to WAITING     │      // Do work          │
│      │     }                          │      condition = true;   │
│      │ }                              │      lock.notify();      │
│      │                                │ }   // Releases lock     │
│      │ ◄───────────────────────────────                         │
│      │ // Wakes up, waits for lock                               │
│      │ // BLOCKED state                                          │
│      │ // Acquires lock, continues                               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Three Ways a Waiting Thread Can Be Awakened:

1. **notify()** - Another thread invokes `notify()` on the same object
2. **Timeout** - The wait time specified in `wait(timeout)` expires
3. **Interrupted** - Another thread calls `interrupt()` on the waiting thread

### Key Points:

- `wait()` **releases the lock** and goes to WAITING state
- `notify()` wakes up **one** waiting thread (selection is JVM-dependent)
- `notifyAll()` wakes up **all** waiting threads
- Awakened thread goes to **BLOCKED** state first (to acquire lock)
- After acquiring lock, thread resumes execution

---

## 14. Thread Joining

The `join()` method makes the **current thread wait** until the specified thread completes.

```java
Thread thread = new Thread(() -> {
    System.out.println(Thread.currentThread());
}, "Our Thread");

thread.start();
thread.join();  // Main thread waits here until "Our Thread" completes

System.out.println("main is exiting");  // Executes after thread completes
```

```
┌─────────────────────────────────────────────────────────────────┐
│                    THREAD JOINING                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Main Thread                      Worker Thread                 │
│       │                                │                         │
│       │ worker.start() ───────────────►│                         │
│       │                                │ // Running              │
│       │ worker.join()                  │ // Running              │
│       │     ⏳ WAITING                 │ // Running              │
│       │     ⏳ WAITING                 │ // Running              │
│       │     ⏳ WAITING                 │ // Done                 │
│       │ ◄──────────────────────────────┘                        │
│       │ // Continues                                             │
│       ▼                                                          │
│   "main is exiting"                                              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 15. Deadlocks

A **deadlock** is a situation where a thread is waiting for an object lock that another thread holds, and that second thread is waiting for a lock that the first thread holds.

### Deadlock Example

```java
String lock1 = "yunus";
String lock2 = "yilmaz";

// Thread 4: Acquires lock1, then tries to acquire lock2
Thread thread4 = new Thread(() -> {
    synchronized (lock1) {
        System.out.println("Thread4: Holding lock1, waiting for lock2...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (lock2) {
            System.out.println("Thread4: Acquired both locks!");
        }
    }
}, "Thread4");

// Thread 5: Acquires lock2, then tries to acquire lock1 (OPPOSITE ORDER!)
Thread thread5 = new Thread(() -> {
    synchronized (lock2) {
        System.out.println("Thread5: Holding lock2, waiting for lock1...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (lock1) {
            System.out.println("Thread5: Acquired both locks!");
        }
    }
}, "Thread5");

// ⚠️ Starting both threads will cause DEADLOCK!
thread4.start();
thread5.start();
```

### Deadlock Visualization

```
┌─────────────────────────────────────────────────────────────────┐
│                         DEADLOCK                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Thread4                              Thread5                   │
│      │                                    │                      │
│      │ synchronized(lock1) {              │                      │
│      │     🔒 Holds lock1                 │ synchronized(lock2) {│
│      │                                    │     🔒 Holds lock2   │
│      │     synchronized(lock2) {          │                      │
│      │         ⏳ Waiting for lock2       │     synchronized(lock1) {
│      │         │                          │         ⏳ Waiting for lock1
│      │         │                          │         │            │
│      │         ▼                          │         ▼            │
│      │    ┌─────────────────────────────────────────────┐       │
│      │    │           💀 DEADLOCK! 💀                    │       │
│      │    │   Thread4 waits for lock2 (held by Thread5) │       │
│      │    │   Thread5 waits for lock1 (held by Thread4) │       │
│      │    │   Neither can proceed!                       │       │
│      │    └─────────────────────────────────────────────┘       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### How to Prevent Deadlock

**Solution 1: Lock Ordering** - Always acquire locks in the **same order**

```java
// ✅ SAFE - Both threads acquire lock1 first, then lock2
Thread threadA = new Thread(() -> {
    synchronized (lock1) {
        synchronized (lock2) {
            // Work
        }
    }
});

Thread threadB = new Thread(() -> {
    synchronized (lock1) {  // Same order!
        synchronized (lock2) {
            // Work
        }
    }
});
```

**Solution 2: Try-Lock with Timeout**

```java
Lock lock1 = new ReentrantLock();
Lock lock2 = new ReentrantLock();

if (lock1.tryLock(1, TimeUnit.SECONDS)) {
    try {
        if (lock2.tryLock(1, TimeUnit.SECONDS)) {
            try {
                // Work with both locks
            } finally {
                lock2.unlock();
            }
        }
    } finally {
        lock1.unlock();
    }
}
```

---

## 16. ReentrantLock

`ReentrantLock` provides more flexibility than `synchronized` blocks with features like **try-lock**, **timed lock**, and **interruptible lock**.

### Why ReentrantLock over synchronized?

| Feature | synchronized | ReentrantLock |
|---------|-------------|---------------|
| **Try Lock** | ❌ No | ✅ Yes |
| **Timed Lock** | ❌ No | ✅ Yes |
| **Interruptible** | ❌ No | ✅ Yes |
| **Fairness** | ❌ No | ✅ Yes |
| **Multiple Conditions** | ❌ No | ✅ Yes |
| **Non-block structured** | ❌ No | ✅ Yes |

### Basic Usage

```java
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
    
    private final ReentrantLock lock = new ReentrantLock();
    private int counter = 0;
    
    public void increment() {
        lock.lock();  // Acquire lock
        try {
            counter++;
        } finally {
            lock.unlock();  // ALWAYS release in finally!
        }
    }
}
```

### Try Lock (Non-Blocking)

```java
public void tryLockDemo() {
    if (lock.tryLock()) {  // Returns immediately
        try {
            System.out.println("Lock acquired!");
            // Critical section
        } finally {
            lock.unlock();
        }
    } else {
        System.out.println("Could not acquire lock, doing something else...");
    }
}
```

### Timed Lock

```java
public void timedLockDemo() throws InterruptedException {
    if (lock.tryLock(5, TimeUnit.SECONDS)) {  // Wait up to 5 seconds
        try {
            System.out.println("Lock acquired within timeout!");
        } finally {
            lock.unlock();
        }
    } else {
        System.out.println("Timeout! Could not acquire lock.");
    }
}
```

### Fair Lock

```java
// Fair lock - threads acquire lock in FIFO order
ReentrantLock fairLock = new ReentrantLock(true);

// Non-fair lock (default) - no ordering guarantee, but faster
ReentrantLock unfairLock = new ReentrantLock(false);
```

### Condition Variables (wait/notify replacement)

```java
public class BoundedBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    
    private final Object[] items = new Object[100];
    private int count = 0;
    
    public void put(Object item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();  // Wait until not full
            }
            items[count++] = item;
            notEmpty.signal();  // Signal that buffer is not empty
        } finally {
            lock.unlock();
        }
    }
    
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();  // Wait until not empty
            }
            Object item = items[--count];
            notFull.signal();  // Signal that buffer is not full
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

---

## 17. ReadWriteLock

`ReadWriteLock` allows **multiple readers** OR **single writer** — perfect for read-heavy scenarios.

```
┌─────────────────────────────────────────────────────────────────┐
│                    READ-WRITE LOCK                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   READ LOCK:                                                     │
│   ┌────────┐ ┌────────┐ ┌────────┐                             │
│   │Reader 1│ │Reader 2│ │Reader 3│  ✅ Multiple readers OK     │
│   └────────┘ └────────┘ └────────┘                             │
│                                                                  │
│   WRITE LOCK:                                                    │
│   ┌────────┐                                                    │
│   │Writer 1│  ✅ Only ONE writer                                │
│   └────────┘                                                    │
│   ┌────────┐ ┌────────┐                                        │
│   │Reader 1│ │Reader 2│  ❌ Readers BLOCKED while writing      │
│   └────────┘ └────────┘                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Implementation

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache {
    
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, Object> cache = new HashMap<>();
    
    // Multiple threads can read simultaneously
    public Object get(String key) {
        rwLock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    // Only one thread can write at a time
    public void put(String key, Object value) {
        rwLock.writeLock().lock();
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    // Read all entries
    public Map<String, Object> getAll() {
        rwLock.readLock().lock();
        try {
            return new HashMap<>(cache);  // Return a copy
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
```

### When to Use

| Scenario | Use |
|----------|-----|
| Read-heavy (90% reads) | ✅ ReadWriteLock |
| Write-heavy | ❌ Use ReentrantLock |
| Equal reads/writes | ❌ Use ReentrantLock |

---

## 18. Atomic Classes

Atomic classes provide **lock-free, thread-safe** operations on single variables using **CAS (Compare-And-Swap)**.

### Available Atomic Classes

| Class | Description |
|-------|-------------|
| `AtomicInteger` | Thread-safe int |
| `AtomicLong` | Thread-safe long |
| `AtomicBoolean` | Thread-safe boolean |
| `AtomicReference<V>` | Thread-safe object reference |
| `AtomicIntegerArray` | Thread-safe int array |
| `LongAdder` | High-performance counter |

### AtomicInteger Example

```java
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter {
    
    private AtomicInteger count = new AtomicInteger(0);
    
    public void increment() {
        count.incrementAndGet();  // Atomic increment
    }
    
    public void decrement() {
        count.decrementAndGet();  // Atomic decrement
    }
    
    public void add(int value) {
        count.addAndGet(value);   // Atomic add
    }
    
    public int get() {
        return count.get();
    }
    
    // Compare-And-Swap
    public boolean compareAndSet(int expected, int newValue) {
        return count.compareAndSet(expected, newValue);
    }
}
```

### AtomicReference Example

```java
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceDemo {
    
    static class User {
        String name;
        int age;
        
        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
    
    private AtomicReference<User> userRef = new AtomicReference<>();
    
    public void updateUser(User newUser) {
        User oldUser;
        do {
            oldUser = userRef.get();
        } while (!userRef.compareAndSet(oldUser, newUser));
    }
    
    public User getUser() {
        return userRef.get();
    }
}
```

### LongAdder (High-Performance Counter)

For **high contention** scenarios, `LongAdder` is faster than `AtomicLong`:

```java
import java.util.concurrent.atomic.LongAdder;

public class HighPerformanceCounter {
    
    private LongAdder counter = new LongAdder();
    
    public void increment() {
        counter.increment();  // Very fast under contention
    }
    
    public long sum() {
        return counter.sum();  // Get total count
    }
}
```

### Comparison

```
┌─────────────────────────────────────────────────────────────────┐
│              ATOMIC vs SYNCHRONIZED vs LOCK                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Atomic (CAS):       ████ Fastest (no blocking)                │
│   ReentrantLock:      ████████ Fast (minimal blocking)          │
│   synchronized:       ████████████ Slower (blocking)            │
│                                                                  │
│   Use Atomic for: Single variable updates                       │
│   Use Lock for: Complex critical sections                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 19. ExecutorService & Thread Pools

Creating threads manually is **expensive**. `ExecutorService` manages a **pool of reusable threads**.

### Why Thread Pools?

```
┌─────────────────────────────────────────────────────────────────┐
│              WITHOUT THREAD POOL                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Task 1 → Create Thread → Execute → Destroy                   │
│   Task 2 → Create Thread → Execute → Destroy                   │
│   Task 3 → Create Thread → Execute → Destroy                   │
│                                                                  │
│   ⚠️ Creating/destroying threads is EXPENSIVE!                  │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│              WITH THREAD POOL                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────────────────────────┐                          │
│   │         Thread Pool (3)          │                          │
│   │  ┌────┐   ┌────┐   ┌────┐       │                          │
│   │  │ T1 │   │ T2 │   │ T3 │       │                          │
│   │  └────┘   └────┘   └────┘       │                          │
│   └──────────────────────────────────┘                          │
│         ▲         ▲         ▲                                   │
│   Task1 ┘   Task2 ┘   Task3 ┘                                   │
│                                                                  │
│   ✅ Threads are REUSED!                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Types of ExecutorService

```java
import java.util.concurrent.*;

public class ExecutorServiceDemo {
    
    public static void main(String[] args) {
        
        // 1. Fixed Thread Pool - Fixed number of threads
        ExecutorService fixedPool = Executors.newFixedThreadPool(4);
        
        // 2. Cached Thread Pool - Creates threads as needed
        ExecutorService cachedPool = Executors.newCachedThreadPool();
        
        // 3. Single Thread Executor - Only one thread
        ExecutorService singleThread = Executors.newSingleThreadExecutor();
        
        // 4. Scheduled Thread Pool - For delayed/periodic tasks
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2);
        
        // 5. Work Stealing Pool (Java 8+) - Uses all available processors
        ExecutorService workStealingPool = Executors.newWorkStealingPool();
    }
}
```

### Submitting Tasks

```java
ExecutorService executor = Executors.newFixedThreadPool(4);

// Submit Runnable (no return value)
executor.submit(() -> {
    System.out.println("Task running on: " + Thread.currentThread().getName());
});

// Submit Callable (returns value)
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(1000);
    return 42;
});

// Get result (blocks until complete)
Integer result = future.get();
System.out.println("Result: " + result);

// Shutdown executor
executor.shutdown();  // Graceful shutdown
executor.awaitTermination(5, TimeUnit.SECONDS);
```

### Scheduled Tasks

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

// Run after delay
scheduler.schedule(() -> {
    System.out.println("Executed after 3 seconds");
}, 3, TimeUnit.SECONDS);

// Run periodically
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Runs every 2 seconds");
}, 0, 2, TimeUnit.SECONDS);

// Run with fixed delay between executions
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Runs with 2 second delay after completion");
}, 0, 2, TimeUnit.SECONDS);
```

### Custom Thread Pool

```java
ThreadPoolExecutor customPool = new ThreadPoolExecutor(
    2,                      // corePoolSize
    4,                      // maximumPoolSize
    60L, TimeUnit.SECONDS,  // keepAliveTime
    new LinkedBlockingQueue<>(100),  // workQueue
    new ThreadFactory() {
        private int count = 0;
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "CustomThread-" + count++);
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // rejectionHandler
);
```

### Shutdown Best Practice

```java
public void shutdownExecutor(ExecutorService executor) {
    executor.shutdown();  // Stop accepting new tasks
    try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();  // Force shutdown
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate");
            }
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

---

## 20. CompletableFuture

`CompletableFuture` provides **non-blocking async programming** with powerful composition.

### Creating CompletableFutures

```java
import java.util.concurrent.CompletableFuture;

// 1. supplyAsync - Returns a value
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    return "Hello";
});

// 2. runAsync - No return value
CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
    System.out.println("Running async task");
});

// 3. With custom executor
ExecutorService executor = Executors.newFixedThreadPool(4);
CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
    return "Hello from custom executor";
}, executor);
```

### Chaining Operations

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")           // Transform result
    .thenApply(String::toUpperCase);        // Chain another transform

System.out.println(future.get());  // "HELLO WORLD"
```

### Combining Futures

```java
// thenCompose - Chain dependent futures (flatMap)
CompletableFuture<String> future1 = CompletableFuture
    .supplyAsync(() -> "user123")
    .thenCompose(userId -> fetchUserDetails(userId));

// thenCombine - Combine two independent futures
CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> "John");
CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> 30);

CompletableFuture<String> combined = nameFuture.thenCombine(ageFuture, 
    (name, age) -> name + " is " + age + " years old");
```

### Waiting for Multiple Futures

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Result 1");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "Result 2");
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "Result 3");

// Wait for ALL to complete
CompletableFuture<Void> allOf = CompletableFuture.allOf(f1, f2, f3);
allOf.join();  // Block until all complete

// Wait for ANY to complete
CompletableFuture<Object> anyOf = CompletableFuture.anyOf(f1, f2, f3);
Object firstResult = anyOf.join();  // Returns first completed result
```

### Exception Handling

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (true) throw new RuntimeException("Oops!");
        return "Success";
    })
    .exceptionally(ex -> {
        System.out.println("Error: " + ex.getMessage());
        return "Default Value";  // Fallback
    });

// Or use handle for both success and failure
CompletableFuture<String> handled = CompletableFuture
    .supplyAsync(() -> "Success")
    .handle((result, ex) -> {
        if (ex != null) {
            return "Error: " + ex.getMessage();
        }
        return result;
    });
```

### Real-World Example: Parallel API Calls

```java
public CompletableFuture<UserDashboard> fetchDashboard(String userId) {
    
    CompletableFuture<User> userFuture = CompletableFuture
        .supplyAsync(() -> fetchUser(userId));
    
    CompletableFuture<List<Order>> ordersFuture = CompletableFuture
        .supplyAsync(() -> fetchOrders(userId));
    
    CompletableFuture<List<Recommendation>> recsFuture = CompletableFuture
        .supplyAsync(() -> fetchRecommendations(userId));
    
    // Combine all results
    return CompletableFuture.allOf(userFuture, ordersFuture, recsFuture)
        .thenApply(v -> new UserDashboard(
            userFuture.join(),
            ordersFuture.join(),
            recsFuture.join()
        ));
}
```

### CompletableFuture Methods Summary

| Method | Description |
|--------|-------------|
| `supplyAsync()` | Run task that returns value |
| `runAsync()` | Run task with no return |
| `thenApply()` | Transform result (map) |
| `thenCompose()` | Chain futures (flatMap) |
| `thenCombine()` | Combine two futures |
| `thenAccept()` | Consume result |
| `thenRun()` | Run action after completion |
| `allOf()` | Wait for all futures |
| `anyOf()` | Wait for any future |
| `exceptionally()` | Handle exception |
| `handle()` | Handle both success/failure |

---

## 21. Synchronization Utilities

Java provides several utility classes for coordinating threads.

### Semaphore - Limit Concurrent Access

```java
import java.util.concurrent.Semaphore;

public class ConnectionPool {
    
    private final Semaphore semaphore;
    
    public ConnectionPool(int maxConnections) {
        this.semaphore = new Semaphore(maxConnections);
    }
    
    public void useConnection() throws InterruptedException {
        semaphore.acquire();  // Wait for permit
        try {
            System.out.println("Using connection: " + Thread.currentThread().getName());
            Thread.sleep(1000);  // Simulate work
        } finally {
            semaphore.release();  // Release permit
        }
    }
}

// Usage
ConnectionPool pool = new ConnectionPool(3);  // Max 3 concurrent connections

for (int i = 0; i < 10; i++) {
    new Thread(() -> {
        try {
            pool.useConnection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }).start();
}
```

### CountDownLatch - Wait for N Events

```java
import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {
    
    public static void main(String[] args) throws InterruptedException {
        
        int workerCount = 3;
        CountDownLatch latch = new CountDownLatch(workerCount);
        
        // Start workers
        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            new Thread(() -> {
                System.out.println("Worker " + workerId + " started");
                try {
                    Thread.sleep(1000 * (workerId + 1));  // Simulate work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Worker " + workerId + " finished");
                latch.countDown();  // Decrement count
            }).start();
        }
        
        System.out.println("Main thread waiting for workers...");
        latch.await();  // Wait until count reaches 0
        System.out.println("All workers finished!");
    }
}
```

```
┌─────────────────────────────────────────────────────────────────┐
│                    COUNT DOWN LATCH                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Count: 3                                                       │
│                                                                  │
│   Worker 1 completes → countDown() → Count: 2                   │
│   Worker 2 completes → countDown() → Count: 1                   │
│   Worker 3 completes → countDown() → Count: 0                   │
│                                                                  │
│   Main thread: await() ─────────────────────► Continues!        │
│                                                                  │
│   ⚠️ Cannot be reset - one-time use only                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### CyclicBarrier - Wait for Each Other

```java
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {
    
    public static void main(String[] args) {
        
        int partySize = 3;
        CyclicBarrier barrier = new CyclicBarrier(partySize, () -> {
            System.out.println("All threads reached barrier! Proceeding...");
        });
        
        for (int i = 0; i < partySize; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + threadId + " doing phase 1");
                    Thread.sleep(1000 * (threadId + 1));
                    
                    System.out.println("Thread " + threadId + " waiting at barrier");
                    barrier.await();  // Wait for all threads
                    
                    System.out.println("Thread " + threadId + " doing phase 2");
                    
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
```

```
┌─────────────────────────────────────────────────────────────────┐
│                    CYCLIC BARRIER                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Thread 1: ████░░░░░░ await() │                                │
│   Thread 2: ████████░░ await() │ ──► All reached ──► Continue  │
│   Thread 3: ██████████ await() │                                │
│                                │                                 │
│                          BARRIER                                │
│                                                                  │
│   ✅ Can be REUSED (cyclic)                                     │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Comparison

| Feature | CountDownLatch | CyclicBarrier |
|---------|----------------|---------------|
| **Reusable** | ❌ No | ✅ Yes |
| **Count Direction** | Counts down | Counts up |
| **Use Case** | Wait for N events | Sync N threads |
| **Reset** | ❌ No | ✅ Automatic |

---

## 22. Virtual Threads (Java 21+)

**Virtual Threads** are lightweight threads that dramatically increase scalability.

### Platform Threads vs Virtual Threads

```
┌─────────────────────────────────────────────────────────────────┐
│           PLATFORM THREADS vs VIRTUAL THREADS                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   PLATFORM THREADS (Traditional):                               │
│   ┌────────────────────────────┐                                │
│   │ OS Thread 1 │ ~1MB Stack  │  1:1 mapping with OS threads   │
│   │ OS Thread 2 │ ~1MB Stack  │  Expensive to create           │
│   │ OS Thread 3 │ ~1MB Stack  │  Limited scalability (~10k)    │
│   └────────────────────────────┘                                │
│                                                                  │
│   VIRTUAL THREADS (Java 21+):                                   │
│   ┌────────────────────────────────────────────┐                │
│   │ VT 1 │ VT 2 │ VT 3 │ ... │ VT 1000000 │    │  M:N mapping  │
│   │  ~KB │  ~KB │  ~KB │     │    ~KB      │    │  Cheap!       │
│   └────────────────────────────────────────────┘                │
│         │         │         │                                   │
│         └─────────┼─────────┘                                   │
│                   ▼                                              │
│   ┌────────────────────────────┐                                │
│   │ Carrier Thread 1 (OS)     │  Few OS threads run many VTs   │
│   │ Carrier Thread 2 (OS)     │                                │
│   └────────────────────────────┘                                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Creating Virtual Threads

```java
// Method 1: Thread.startVirtualThread()
Thread vThread = Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread: " + Thread.currentThread());
});

// Method 2: Thread.ofVirtual()
Thread vThread2 = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        System.out.println("Named virtual thread");
    });

// Method 3: Virtual Thread Factory
ThreadFactory factory = Thread.ofVirtual().name("worker-", 0).factory();
Thread vThread3 = factory.newThread(() -> {
    System.out.println("Factory-created virtual thread");
});
vThread3.start();
```

### Virtual Thread Executor

```java
// Creates a new virtual thread for each task
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    
    // Submit 10,000 tasks - each gets its own virtual thread!
    for (int i = 0; i < 10_000; i++) {
        final int taskId = i;
        executor.submit(() -> {
            Thread.sleep(1000);  // Simulate I/O
            System.out.println("Task " + taskId + " completed");
            return taskId;
        });
    }
    
}  // Auto-shutdown when try block exits
```

### Scalability Demo

```java
public class VirtualThreadScalability {
    
    public static void main(String[] args) throws Exception {
        
        int taskCount = 100_000;
        
        // With Virtual Threads - handles 100k concurrent tasks easily!
        long start = System.currentTimeMillis();
        
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    Thread.sleep(1000);  // Simulate I/O
                    return null;
                });
            }
        }
        
        long duration = System.currentTimeMillis() - start;
        System.out.println("Completed " + taskCount + " tasks in " + duration + "ms");
        // ~1-2 seconds (not 100,000 seconds!)
    }
}
```

### When to Use Virtual Threads

| Use Case | Recommendation |
|----------|----------------|
| **I/O-bound tasks** | ✅ Virtual Threads |
| **CPU-bound tasks** | ❌ Platform Threads |
| **High concurrency** | ✅ Virtual Threads |
| **Thread-local heavy** | ⚠️ Use carefully |
| **Synchronized blocks** | ⚠️ Avoid (use ReentrantLock) |

### Migration from Platform Threads

```java
// Before (Platform Threads)
ExecutorService executor = Executors.newFixedThreadPool(200);

// After (Virtual Threads) - Just change one line!
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

### Best Practices for Virtual Threads

```java
// ✅ DO: Use for I/O-bound operations
Thread.startVirtualThread(() -> {
    // HTTP calls, database queries, file I/O
    httpClient.send(request);
});

// ✅ DO: Use try-with-resources with executor
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Submit tasks
}

// ❌ DON'T: Use synchronized (pins the carrier thread)
synchronized (lock) {  // Avoid!
    // ...
}

// ✅ DO: Use ReentrantLock instead
lock.lock();
try {
    // ...
} finally {
    lock.unlock();
}

// ❌ DON'T: Pool virtual threads (they're cheap!)
// Just create new ones as needed
```

---

## 23. Best Practices

### ✅ DO

1. **Prefer Runnable over Thread**
   ```java
   // ✅ Good
   Thread t = new Thread(() -> doWork());
   
   // ❌ Avoid
   class MyThread extends Thread { }
   ```

2. **Always call start(), never run()**
   ```java
   thread.start();  // ✅ Creates new thread
   // thread.run();  // ❌ Runs in current thread
   ```

3. **Use meaningful thread names**
   ```java
   new Thread(task, "OrderProcessor-1").start();
   ```

4. **Minimize synchronized blocks**
   ```java
   public void method() {
       // Non-critical code
       synchronized (lock) {
           // Only critical section
       }
       // More non-critical code
   }
   ```

5. **Handle InterruptedException properly**
   ```java
   try {
       Thread.sleep(1000);
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt();  // Restore flag
       return;
   }
   ```

6. **Always acquire locks in the same order** to prevent deadlocks

7. **Use `wait()` in a loop** (to handle spurious wakeups)
   ```java
   synchronized (lock) {
       while (!condition) {
           lock.wait();
       }
   }
   ```

### ❌ DON'T

1. **Don't use stop(), suspend(), resume()** - They're deprecated
2. **Don't synchronize on String literals** - They're interned (shared)
3. **Don't hold locks longer than necessary**
4. **Don't ignore InterruptedException**
5. **Don't synchronize on non-final fields**

---

## 📁 Project Structure

```
java-thread-demo/
├── pom.xml
├── README.md
└── src/main/java/com/threadsdemo/
    ├── JavaThreadDemoApplication.java    # Main class with all demos
    │
    ├── multithreading/
    │   ├── Thread1.java                  # Extends Thread
    │   ├── Thread2.java                  # Implements Runnable
    │   └── Stack.java                    # Thread-safe Stack (synchronized)
    │
    └── advanced/
        ├── ReentrantLockDemo.java        # Lock with tryLock, conditions
        ├── ReadWriteLockDemo.java        # Multiple readers, single writer
        ├── AtomicDemo.java               # AtomicInteger, AtomicReference, LongAdder
        ├── ExecutorServiceDemo.java      # Thread pools, schedulers
        ├── CompletableFutureDemo.java    # Async programming
        ├── SynchronizationUtilitiesDemo.java  # Semaphore, CountDownLatch, CyclicBarrier
        └── VirtualThreadsDemo.java       # Java 21+ virtual threads
```

---

## 🚀 Running the Code

```bash
# Compile
mvn compile

# Run main demo
mvn exec:java -Dexec.mainClass="com.threadsdemo.JavaThreadDemoApplication"

# Run specific advanced demos
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.ReentrantLockDemo"
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.ReadWriteLockDemo"
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.AtomicDemo"
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.ExecutorServiceDemo"
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.CompletableFutureDemo"
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.SynchronizationUtilitiesDemo"
mvn exec:java -Dexec.mainClass="com.threadsdemo.advanced.VirtualThreadsDemo"
```

---

## 📊 Quick Reference

| Concept | Code |
|---------|------|
| Create Thread (extends) | `class T extends Thread` |
| Create Thread (runnable) | `new Thread(() -> {})` |
| Start Thread | `thread.start()` |
| Sleep | `Thread.sleep(1000)` |
| Wait for completion | `thread.join()` |
| Get current thread | `Thread.currentThread()` |
| Get thread name | `Thread.currentThread().getName()` |
| Set daemon | `thread.setDaemon(true)` |
| Get priority | `thread.getPriority()` |
| Synchronized method | `public synchronized void m()` |
| Synchronized block | `synchronized(lock) { }` |
| Wait | `lock.wait()` |
| Notify | `lock.notify()` |
| Notify all | `lock.notifyAll()` |
| Check state | `thread.getState()` |
| ReentrantLock | `lock.lock(); try{} finally{lock.unlock();}` |
| ReadWriteLock | `rwLock.readLock().lock()` |
| AtomicInteger | `atomicInt.incrementAndGet()` |
| ExecutorService | `Executors.newFixedThreadPool(4)` |
| CompletableFuture | `CompletableFuture.supplyAsync()` |
| Semaphore | `semaphore.acquire(); semaphore.release();` |
| CountDownLatch | `latch.countDown(); latch.await();` |
| CyclicBarrier | `barrier.await()` |
| Virtual Thread | `Thread.startVirtualThread(() -> {})` |
| Virtual Executor | `Executors.newVirtualThreadPerTaskExecutor()` |

---

## 🎓 Key Takeaways

| Concept | Key Point |
|---------|-----------|
| **Thread** | Smallest unit of execution |
| **Runnable** | Preferred way to define thread task |
| **Daemon** | Background thread, doesn't prevent JVM exit |
| **synchronized** | Ensures mutual exclusion |
| **Same Lock** | Blocks ALL synchronized methods on that lock |
| **wait()** | Releases lock, goes to WAITING |
| **notify()** | Wakes up one waiting thread |
| **sleep()** | Does NOT release lock |
| **join()** | Wait for thread to complete |
| **Deadlock** | Circular waiting for locks |
| **ReentrantLock** | Flexible lock with tryLock, timeout |
| **ReadWriteLock** | Multiple readers OR single writer |
| **Atomic** | Lock-free thread-safe operations |
| **ExecutorService** | Reusable thread pool |
| **CompletableFuture** | Non-blocking async programming |
| **Semaphore** | Limit concurrent access |
| **CountDownLatch** | Wait for N events (one-time) |
| **CyclicBarrier** | Sync N threads (reusable) |
| **Virtual Threads** | Lightweight, scalable threads (Java 21+) |

---

**Happy Threading! 🚀**
