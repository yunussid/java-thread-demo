package com.threadsdemo.multithreading;

public class Thread2 implements Runnable{

//    public Thread2(String threadName){
//        Thread.currentThread().setName(threadName);
//    }

    @Override
    public void run() {
        for (int i=0;i<5;i++){
            System.out.println("Inside the thread "+Thread.currentThread().getName() + " with value of i as "+i);
        }
    }
}
