package com.threadsdemo;

import com.threadsdemo.multithreading.Stack;
import com.threadsdemo.multithreading.Thread1;
import com.threadsdemo.multithreading.Thread2;

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

    public static void main(String[] args) throws InterruptedException {

        System.out.println("main is started");

        // Use Thread1 instead of Thread
        Thread thread1 = new Thread1("threadThread");
        thread1.setDaemon(true);
        thread1.start();

        Thread thread2=new Thread(()->{
            for (int i=0;i<5;i++){
                System.out.println("Inside the thread "+Thread.currentThread().getName() + " with value of i as "+i);
            }
        },"threadRunnable");
        thread2.start();

        Stack stack=new Stack(5);
        new Thread(()-> {
            int counter = 0;
            while (++counter < 10)
                System.out.println("Pushed: " + stack.push(100));
        },"Pusher").start();

        new Thread(()->{
            int counter=0;
            while(++ counter < 10)
                System.out.println("Popped: "+ stack.pop());
        },"Popper").start();

        Thread thread3=new Thread(()->{
            try{
                Thread.sleep(1);
                for(int i=10000;i>0;i--);
            }catch (InterruptedException e){
                    System.out.println("Thread was interrupted");
            }
        },"States");
        thread3.start();
        while (true){
            Thread.State state=thread3.getState();
            System.out.println(state);
            if(state==Thread.State.TERMINATED){
                break;
            }
        }

        Thread thread=new Thread(()->{
            System.out.println(Thread.currentThread());
        },"Our Thread");
        thread.start();
        thread.join();
        System.out.println("main is exiting");

        String lock1="yunus";
        String lock2="yilmaz";
        Thread thread4=new Thread(() ->{
            synchronized (lock1){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (lock2){
                    System.out.println("lock acquired");
                }
            }
        },"Thread4");
        Thread thread5=new Thread(() ->{
            synchronized (lock2){
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized (lock1){
                    System.out.println("lock acquired");
                }
            }
        },"Thread4");
        System.out.println(thread.getPriority());
    }
}