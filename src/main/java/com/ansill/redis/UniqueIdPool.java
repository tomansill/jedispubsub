package com.ansill.redis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class that keeps tracks of unique ids - supports ((2^32) - 1) many ids
 */
@SuppressWarnings("WeakerAccess")
public class UniqueIdPool{

    /**
     * Pool of claimed ids
     */
    private final Set<Integer> pool = new HashSet<>();

    /**
     * Flipping counter
     */
    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Default constructor
     */
    public UniqueIdPool(){
    }

    /**
     * Draws an unique id
     *
     * @return unique id
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public synchronized int draw(){

        // Declare id
        int id;

        // Loop until next ID is available
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
    public synchronized boolean surrender(int id){
        return this.pool.remove(id);
    }
}
