package com.ansill.redis;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/** Simple blocking channel */
public class Channel<T>{

    /**
     * Queue
     */
    @Nonnull
    private final Queue<T> queue = new ConcurrentLinkedQueue<>();

    /** Resettable CountDownLatch */
    @Nonnull
    private final ResetableCountDownLatch rcdl = new ResetableCountDownLatch(1);

    /** Default constructor */
    public Channel(){
    }

    /**
     * Enqueues item in the queue
     *
     * @param item item
     */
    public synchronized void enqueue(@Nonnull T item){
        this.queue.add(item);
        this.rcdl.countDown();
    }

    /**
     * Polls item from the queue
     *
     * @param time time duration to wait
     * @param unit time unit to wait
     * @return optional value of item, if it is empty, then the wait has been timed out
     * @throws InterruptedException thrown if the waiting thread was interrupted
     */
    @Nonnull
    public Optional<T> poll(@Nonnegative long time, @Nonnull TimeUnit unit)
    throws InterruptedException{

        // Wait for it
        if(!this.rcdl.await(time, unit)) return Optional.empty();

        // Synchronize
        synchronized(this){

            // Get item
            T item = this.queue.poll();

            // If queue is empty, reset the RCDL
            if(item == null || this.queue.isEmpty()) this.rcdl.reset(1);

            // Return it
            return Optional.ofNullable(item);
        }
    }
}