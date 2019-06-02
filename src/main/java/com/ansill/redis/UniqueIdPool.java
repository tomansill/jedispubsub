package com.ansill.redis;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Class that keeps tracks of unique ids - supports ((2^32) - 1) many ids */
class UniqueIdPool{

    /** Pool of claimed ids */
    @Nonnull
    private final Set<Integer> pool = new HashSet<>();

    /** Flipping counter */
    @Nonnull
    private final AtomicInteger counter = new AtomicInteger(0);

    /** Default constructor */
    UniqueIdPool(){
    }

    /**
     * Draws an unique id
     *
     * @return unique id
     */
    @SuppressWarnings("StatementWithEmptyBody")
    synchronized int draw(){

        // Throw if pool is full
        if(this.pool.size() == Integer.MAX_VALUE) throw new RuntimeException("Pool is full");

        // Declare id
        int id;

        // Loop until next id is available
        while(!this.pool.add(id = this.counter.getAndIncrement())) ;

        // Return id
        return id;
    }

    /**
     * Surrenders id
     *
     * @param id id to surrender
     * @return true if id is valid, false if it has never been claimed
     */
    @SuppressWarnings("UnusedReturnValue")
    synchronized boolean surrender(int id){
        return this.pool.remove(id);
    }
}
