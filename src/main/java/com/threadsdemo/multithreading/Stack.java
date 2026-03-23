package com.threadsdemo.multithreading;

public class Stack {

    private int[] array;
    private int top;
    private int capacity;

    public Stack(int capacity){
        array = new int[capacity];
        top=-1;
    }

    public boolean isFull(){
        return top == array.length-1;
    }

    public boolean isEmpty(){
        return top==-1;
    }

    public synchronized boolean push(int element){
//        synchronized(this) {
            if(isFull()){
                return false;
            }

            ++top;
            try{ Thread.sleep(1000); } catch (Exception e){}
            array[top] = element;
            return true;
//        }
    }

    public synchronized int pop(){
//        synchronized (this){
            if (isEmpty()) return Integer.MIN_VALUE;

            int obj = array[top];
            array[top] = Integer.MIN_VALUE;

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            --top;
            return obj;
        }
//    }
}
