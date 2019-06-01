package com.ansill.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Pubsub manager */
public final class JedisPubSubManager implements AutoCloseable{

    /** Name of channel that is always held open */
    @Nonnull
    private static final String DEFAULT_CHANNEL_NAME = "DEFAULT_INACTIVE_CHANNEL";

    /** Connection */
    @Nonnull
    private final Jedis connection;

    /** Consumer count map */
    @Nonnull
    private final Map<String,UniqueIdPool> counter_map = new ConcurrentHashMap<>();

    /** Consumer map */
    @Nonnull
    private final Map<String,Map<Integer,Consumer<String>>> consumer_map = new ConcurrentHashMap<>();

    /** PubSub object */
    @Nonnull
    private final PubSub pubsub;

    /** Subscription count */
    @Nonnull
    private final AtomicLong subscriptions = new AtomicLong(0);

    /** Closed CDL */
    @Nonnull
    private final CountDownLatch closed_cdl = new CountDownLatch(1);

    /**
     * Creates pub sub manager
     *
     * @param hostname hostname
     * @param port     port
     */
    public JedisPubSubManager(@Nonnull String hostname, @Nonnegative int port){

        // Create new exclusive connection
        this.connection = new Jedis(hostname, port);

        // Ping it (test the connection)
        this.connection.ping("Hello!");

        // Set up CDLs
        CountDownLatch ready_cdl = new CountDownLatch(1);
        CountDownLatch message_cdl = new CountDownLatch(1);

        // Create pubsub
        this.pubsub = new PubSub(this.consumer_map, ready_cdl, message_cdl);

        // Start the blocking subscription in another thread
        new Thread(() -> {

            // Subscribe to default channel name - method will block until unsubscribe() is called
            this.connection.subscribe(this.pubsub, DEFAULT_CHANNEL_NAME);

            // Countdown to indicate that subscription is closed
            this.closed_cdl.countDown();

        }).start();

        try{

            // Wait for pubsub to become ready
            ready_cdl.await();

            // Create test connection
            try(Jedis connection = new Jedis(hostname, port)){

                // Send test message to ensure that pubsub is ready - poll as much as needed
                int counts = 0;
                int max_failure = 20;
                do{

                    // Stop if failing too much
                    if(counts++ == max_failure) throw new RuntimeException("Failed to acknowledge the PubSub manager!");

                    // Publish
                    connection.publish(DEFAULT_CHANNEL_NAME, "JedisPubSubManager helloing!");

                }while(!message_cdl.await(100, TimeUnit.MILLISECONDS)); // Wait for pub sub to receive message
            }

        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Subscribes to a channel and receive a subscription reference so the subscription can be cancelled later on
     *
     * @param channel  channel name
     * @param consumer consumer function
     * @return subscription reference
     */
    @Nonnull
    public Subscription subscribe(@Nonnull String channel, @Nonnull Consumer<String> consumer){

        // Error if closed
        if(this.closed_cdl.getCount() == 0) throw new IllegalStateException("JedisPubSubManager is closed!");

        // Get unique id
        int id = this.counter_map.computeIfAbsent(channel, key -> new UniqueIdPool()).draw();

        // Subscribe to channel
        this.consumer_map.computeIfAbsent(channel, key -> {
            this.pubsub.subscribe(channel);
            return new ConcurrentHashMap<>();
        }).put(id, consumer);

        // Count up subscription count
        this.subscriptions.incrementAndGet();

        // Return subscription with runnable that will remove consumer and surrenders id at the end of subscription
        return new Subscription(() -> {

            // Update map
            this.consumer_map.computeIfPresent(channel, (key, value) -> {

                // Remove
                value.remove(id);

                // Surrender
                this.counter_map.get(channel).surrender(id);

                // Decrement subscription count
                this.subscriptions.decrementAndGet();

                // If channel map is not empty, then leave it alone
                if(!value.isEmpty()) return value;

                // Otherwise unsubscribe the channel
                if(this.closed_cdl.getCount() != 0) this.pubsub.unsubscribe(channel);

                // Clean up map
                this.counter_map.remove(channel);

                // Return null to remove this value
                return null;

            });
        });
    }

    /**
     * Get subscription count
     *
     * @return subscription count
     */
    public long getSubscriptionCount(){
        return this.subscriptions.get();
    }

    @Override
    public void close(){

        // Unsubscribe
        this.pubsub.unsubscribe();

        // Wait for main subscription thread to exit
        try{
            this.closed_cdl.await();
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }

        // Close the connection
        this.connection.close();
    }

    /** Customized JedisPubSub class */
    private static class PubSub extends JedisPubSub{

        /** Channel function map */
        @Nonnull
        private final Map<String,Map<Integer,Consumer<String>>> channel_function_map;

        /** CDL to ensure that it's working */
        @Nonnull
        private final CountDownLatch cdl;

        /**
         * PubSub constructor
         *
         * @param channel_function_map channel map
         * @param ready_cdl            CDL for ready
         * @param message_cdl          CDL for message
         */
        private PubSub(
                @Nonnull Map<String,Map<Integer,Consumer<String>>> channel_function_map,
                @Nonnull CountDownLatch ready_cdl,
                @Nonnull CountDownLatch message_cdl
        ){
            this.channel_function_map = channel_function_map;
            this.cdl = message_cdl;
            ready_cdl.countDown();
        }

        @Override
        public void onMessage(String channel, String message){

            // If null, ignore
            if(channel == null) return;

            // If channel doesn't exist
            this.channel_function_map.computeIfAbsent(channel, key -> {

                // Quietly ignore default channel name
                if(key.equals(DEFAULT_CHANNEL_NAME)) return Collections.singletonMap(
                        null,
                        inner -> this.cdl.countDown()
                );

                // Otherwise warn about other channels
                return Collections.singletonMap(
                        null,
                        inner -> System.err.println("Unexpected channel '" + key + "' showed up the pubsub manager")
                );

            }).values().parallelStream().forEach(item -> item.accept(message));
        }
    }
}
