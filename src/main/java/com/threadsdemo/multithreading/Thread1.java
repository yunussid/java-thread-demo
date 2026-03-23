package com.threadsdemo.multithreading;

public class Thread1 extends Thread{

    public Thread1(String threadName){
        super(threadName);
    }

    @Override
    public void run(){
        for (int i=0;i<5;i++){
            System.out.println("Inside the thread "+Thread.currentThread().getName() + " with value of i as "+i);
        }
    }
}
