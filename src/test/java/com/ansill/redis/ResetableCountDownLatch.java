package com.ansill.redis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ResetableCountDownLatch{

    private final AtomicReference<CountDownLatch> cdl = new AtomicReference<>();

    private int initial_value = 0;

    public ResetableCountDownLatch(final int initial_value){
        this.initial_value = initial_value;
        this.cdl.set(new CountDownLatch(initial_value));
    }

    public void countDown(){
        this.cdl.get().countDown();
    }

    public void await() throws InterruptedException{
        this.cdl.get().await();
    }

    public boolean await(final long time, final TimeUnit unit) throws InterruptedException{
        return this.cdl.get().await(time, unit);
    }

    public void reset(){
        this.cdl.set(new CountDownLatch(initial_value));
    }

    public void reset(final int new_value){
        this.initial_value = new_value;
        this.reset();
    }
}
