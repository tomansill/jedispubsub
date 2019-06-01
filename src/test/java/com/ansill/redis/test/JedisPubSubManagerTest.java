package com.ansill.redis.test;

import com.ansill.redis.Channel;
import com.ansill.redis.JedisPubSubManager;
import com.ansill.redis.ServerUtility;
import com.ansill.redis.Subscription;
import org.junit.jupiter.api.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("unused")
class JedisPubSubManagerTest{

    private static ServerUtility.Server SERVER;

    @BeforeAll
    static void setUp(){
        SERVER = ServerUtility.getServer();
    }

    @AfterAll
    static void tearDown(){
        SERVER.close();
    }

    static String genString(){
        return (Math.random() + "").replace(".", "");
    }

    @Test
    void testNonexistentServer(){

        assertThrows(JedisConnectionException.class, () -> new JedisPubSubManager("nonexistent", 1));

    }

    @DisplayName("Simple subscription test")
    @RepeatedTest(3)
    void simpleSubscriptionTest() throws InterruptedException, TimeoutException{

        // Get manager
        try(JedisPubSubManager manager = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Create channel object
            Channel<String> channel = new Channel<>();

            // Set up channel name
            String channel_name = "my_channel";

            // Subscribe to something
            try(Subscription subscription = manager.subscribe(channel_name, channel::enqueue)){

                // Set up expected message
                String message_one = "hello!";
                String message_two = "world!";

                // Get a connection and say something
                try(Jedis connection = SERVER.getConnection()){
                    connection.publish(channel_name, message_one);
                    connection.publish(channel_name, message_two);
                }

                // Check the channel
                String incoming = channel.poll(500, TimeUnit.MILLISECONDS)
                                         .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message_one, incoming);

                // Check the channel
                incoming = channel.poll(500, TimeUnit.MILLISECONDS)
                                  .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message_two, incoming);
            }
        }
    }

    @DisplayName("Multiple consumers per channel test")
    @RepeatedTest(3)
    void multipleConsumerPerChannelTest() throws InterruptedException, TimeoutException{

        // Get manager
        try(JedisPubSubManager manager = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Create channel objects
            Channel<String> channel1 = new Channel<>();
            Channel<String> channel2 = new Channel<>();

            // Custom functions
            String id = genString();
            Function<String,String> fun1 = message -> message + "fun";
            Function<String,String> fun2 = message -> message + id;

            // Set up channel name
            String channel_name = "channel:" + genString();

            // Subscribe to something
            try(
                    Subscription subscription_one = manager.subscribe(
                            channel_name,
                            message -> channel1.enqueue(fun1.apply(message))
                    );
                    Subscription subscription_two = manager.subscribe(
                            channel_name,
                            message -> channel2.enqueue(fun2.apply(message))
                    )
            ){

                // Set up expected message
                String message = "hello!";

                // Get a connection and say something
                try(Jedis connection = SERVER.getConnection()){
                    connection.publish(channel_name, message);
                }

                // Check the channel
                String incoming = channel1.poll(500, TimeUnit.MILLISECONDS)
                                          .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(fun1.apply(message), incoming);

                // Check the channel
                incoming = channel2.poll(500, TimeUnit.MILLISECONDS)
                                   .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(fun2.apply(message), incoming);
            }
        }
    }

    @DisplayName("Multiple channel test")
    @RepeatedTest(3)
    void multipleChannelTest() throws InterruptedException, TimeoutException{

        // Get manager
        try(JedisPubSubManager manager = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Create channel objects
            Channel<String> channel_one = new Channel<>();
            Channel<String> channel_two = new Channel<>();

            // Set up channel names
            String channel_one_name = "channel" + genString();
            String channel_two_name = "channel" + genString();

            // Set up subscriptions
            try(
                    Subscription subscription_one = manager.subscribe(channel_one_name, channel_one::enqueue);
                    Subscription subscription_two = manager.subscribe(channel_two_name, channel_two::enqueue)
            ){

                // Set up expected message
                String message_one = "hello!" + genString();
                String message_two = "hello!" + genString();

                // Get a connection and say something
                try(Jedis connection = SERVER.getConnection()){
                    connection.publish(channel_one_name, message_one);
                    connection.publish(channel_two_name, message_two);
                }

                // Check the channel
                String incoming = channel_one.poll(500, TimeUnit.MILLISECONDS)
                                             .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message_one, incoming);

                // Check the channel
                incoming = channel_two.poll(500, TimeUnit.MILLISECONDS)
                                      .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message_two, incoming);
            }
        }
    }

    @DisplayName("Multiple channel test")
    @RepeatedTest(3)
    void multipleManagerTest() throws InterruptedException, TimeoutException{

        // Set up message
        String message = "hello world!";

        // Get manager
        try(JedisPubSubManager manager_one = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Create channel object
            Channel<String> channel_one = new Channel<>();

            // Set up channel name
            String channel_name = "channel" + genString();

            // Set up subscription for manager one
            try(Subscription subscription_one = manager_one.subscribe(channel_name, channel_one::enqueue)){

                // Get new manager
                try(JedisPubSubManager manager_two = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

                    // Create channel object
                    Channel<String> channel_two = new Channel<>();

                    // Set up subscription for manager two
                    try(Subscription subscription_two = manager_two.subscribe(channel_name, channel_two::enqueue)){

                        // Get a connection and say something
                        try(Jedis connection = SERVER.getConnection()){
                            connection.publish(channel_name, message);
                        }

                        // Check the channel
                        String incoming = channel_two.poll(500, TimeUnit.MILLISECONDS)
                                                     .orElseThrow(() -> new TimeoutException("Timed Out!"));

                        // Assert it
                        assertEquals(message, incoming);

                    }
                }


                // Check the channel
                String incoming = channel_one.poll(500, TimeUnit.MILLISECONDS)
                                             .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message, incoming);
            }
        }
    }

    @DisplayName("Subscribing after closing the manager test")
    @Test
    void testImproperUse() throws InterruptedException, TimeoutException{

        // Declare manager
        JedisPubSubManager manager = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort());

        // Get manager
        try{

            // Create channel object
            Channel<String> channel = new Channel<>();

            // Set up channel name
            String channel_name = "my_channel";

            // Subscribe to something
            try(Subscription subscription = manager.subscribe(channel_name, channel::enqueue)){

                // Set up expected message
                String message = "hello!";

                // Get a connection and say something
                try(Jedis connection = SERVER.getConnection()){
                    connection.publish(channel_name, message);
                }

                // Check the channel
                String incoming = channel.poll(500, TimeUnit.MILLISECONDS)
                                         .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message, incoming);
            }

        }finally{
            manager.close();
        }

        // Now try subscribe something
        assertThrows(IllegalStateException.class, () -> manager.subscribe(genString(), System.err::println));

    }

    @DisplayName("Unsubscribe test")
    @Test
    void unsubscribeTest() throws InterruptedException, TimeoutException{

        // Get manager
        try(JedisPubSubManager manager = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Create channel object
            Channel<String> channel = new Channel<>();

            // Set up channel name
            String channel_name = genString();

            // Subscribe to something
            try(Subscription subscription = manager.subscribe(channel_name, channel::enqueue)){

                // Set up expected message
                String message = "hello!";

                // Get a connection and say something
                try(Jedis connection = SERVER.getConnection()){
                    connection.publish(channel_name, message);
                }

                // Check the channel
                String incoming = channel.poll(500, TimeUnit.MILLISECONDS)
                                         .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message, incoming);
            }

            // Assuming that subscription has ended, try publish again
            try(Jedis connection = SERVER.getConnection()){
                connection.publish(channel_name, genString());
            }

            // Channel<String> should not retrieve anything
            assertEquals(Optional.empty(), channel.poll(1, TimeUnit.SECONDS));
        }
    }

    @DisplayName("Unsubscribe multiple test")
    @Test
    void unsubscribeMultipleTest() throws Throwable{

        // Get manager
        try(JedisPubSubManager manager = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Create channel objects
            Channel<String> channel_one = new Channel<>();
            Channel<String> channel_two = new Channel<>();

            // Set up channel name
            String channel_name = genString();

            // Subscribe to something
            try(Subscription subscription_one = manager.subscribe(channel_name, channel_one::enqueue)){

                // Subscribe to something
                try(Subscription subscription_two = manager.subscribe(channel_name, channel_two::enqueue)){

                    // Set up expected message
                    String message = "hello!";

                    // Get a connection and say something
                    try(Jedis connection = SERVER.getConnection()){
                        connection.publish(channel_name, message);
                    }

                    // Check the channel
                    String incoming = channel_one.poll(500, TimeUnit.MILLISECONDS)
                                                 .orElseThrow(() -> new TimeoutException("Timed Out!"));

                    // Assert it
                    assertEquals(message, incoming);


                    // Check the channel
                    incoming = channel_two.poll(500, TimeUnit.MILLISECONDS)
                                          .orElseThrow(() -> new TimeoutException("Timed Out!"));

                    // Assert it
                    assertEquals(message, incoming);
                }

                // Set up expected message
                String message = "hello!" + genString();

                // Get a connection and say something
                try(Jedis connection = SERVER.getConnection()){
                    connection.publish(channel_name, message);
                }

                // Check the channel
                String incoming = channel_one.poll(500, TimeUnit.MILLISECONDS)
                                             .orElseThrow(() -> new TimeoutException("Timed Out!"));

                // Assert it
                assertEquals(message, incoming);

                // Assuming subscription two has ended, channel two should not retrieve anything
                assertEquals(Optional.empty(), channel_two.poll(1, TimeUnit.SECONDS));

            }

            // Assuming that subscription has ended, try publish again
            try(Jedis connection = SERVER.getConnection()){
                connection.publish(channel_name, genString());
            }

            // Channel<String>s should not retrieve anything
            assertEquals(Optional.empty(), channel_one.poll(1, TimeUnit.SECONDS));
            assertEquals(Optional.empty(), channel_two.poll(1, TimeUnit.SECONDS));
        }
    }

    @DisplayName("Subscription count test")
    @Test
    void subscriptionCountTest(){

        // Channel name
        String channel_name = genString();

        // Get manager
        try(JedisPubSubManager manager_one = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

            // Get number
            int subs_one = 10;

            // Subscriptions
            List<Subscription> subscription_one = new LinkedList<>();

            // Get manager
            try(JedisPubSubManager manager_two = new JedisPubSubManager(SERVER.getHostname(), SERVER.getPort())){

                // Get number
                int subs_two = 10;

                List<Subscription> subscription_two = new LinkedList<>();

                try{

                    IntStream.range(0, subs_one).forEach(i -> subscription_one.add(manager_one.subscribe(
                            channel_name,
                            item -> {
                            }
                    )));

                    IntStream.range(0, subs_two).forEach(i -> subscription_two.add(manager_two.subscribe(
                            channel_name,
                            item -> {
                            }
                    )));

                    // One more sub
                    try(
                            Subscription subscription = manager_one.subscribe(channel_name, item -> {
                            })
                    ){

                        // Spam something
                        try(Jedis connection = SERVER.getConnection()){
                            connection.publish(channel_name, genString());
                        }

                        // Assert correct subscriptions
                        assertEquals(subs_one + 1, manager_one.getSubscriptionCount());
                    }

                    // Assert correct subscriptions
                    assertEquals(subs_one, manager_one.getSubscriptionCount());

                    // Assert correct subscriptions
                    assertEquals(subs_two, manager_one.getSubscriptionCount());

                }finally{
                    subscription_two.forEach(Subscription::close);
                }

            }finally{
                subscription_one.forEach(Subscription::close);
            }

            // Assert correct subscriptions
            assertEquals(0, manager_one.getSubscriptionCount());
        }
    }
}