# JedisPubSubManager

An easy-to-use Publish&Subscription Manager for [Jedis](https://github.com/xetorthio/jedis) library. 

By Tom Ansill

## Motivation

The current PubSub implementation in Jedis library is not quite easy to use it requires you to implement custom class and its `subscribe(String... channel)` method blocks so it requires you to create new thread to let that blocking method to continue as you use it. Also, Jedis' `unsubscribe()` method is bit buggy and will break things. 
I needed an easy-to-use library that wraps Jedis's PubSub implementation and let me to use PubSub functionality without any headaches.

## Prerequisites

* Java 8 or better
* Maven
* [My Java Validation library](https://github.com/tomansill/JavaValidation)

## Download

**No Maven Repository available yet ):**

For now, you need to build and install it on your machine.

```bash
$ git clone https://github.com/tomansill/jedispubsub
$ cd jedispubsub
$ mvn install
```

Then include the dependency in your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.ansill.redis</groupId>
    <artifactId>jedis-pubsub-manager</artifactId>
    <version>0.1.0</version>
</dependency>
```

## How to use

First, you need a manager to manage your PubSub.

It's fairly simple to initialize `JedisPubSubManager`. You just call its constructor to start the manager like this:

```
JedisPubSubManager manager = new JedisPubSUbManager("localhost", 6379);

// Stuff

manager.close(); // Don't forget to close when you're done with it
```

`JedisPubSubManager` is an `AutoCloseable` resource so it's recommended that you use try-with-resources construct to clean up the manager after you're done with it.

After you have the manager set up, then you can start subscribing to channels.


You subscribe channel with desired channel name and your `Consumer<String>` function that will be run when someone has published a message on the channel.
When you subscribe to a channel, you will get `Subscription` object in return. 
You need that `Subscription` object to cancel *(unsubscribe)* your subscription later. 

```
// Initializes manager in a try-with-resources block
try(JedisPubSubManager manager = new JedisPubSubManager("localhost", 6379)){
    
    // Creates subscription that subscribes to channel named "my_channel" and a custom consumer function
    Subscription subscription = manager.subscribe("my_channel", message -> {
       
        if(message.equals("hello!")) System.out.println("Somebody says hello!");
        
        else System.out.println("Somebody said '" + message + "'");
        
    });
    
    // Do other things ...
    
    // Finished with subscription
    subscription.cancel(); // Don't forget to unsubscribe by calling cancel()
    
}
```

`Subscription` is also an `AutoCloseable` resource so you can use it with try-with-resources to have it automatically unsubscribe when you're done with the subscription.

You can put as many subscriptions on single channel as much as you want.

## Examples

**TODO:** See JUnit tests for examples for now

## Known issues

* Doesn't currently support cluster configuration. *However, it is trivial to implement this*

* Limit of `Integer.MAX_VALUE` `Consumers` in a **single** channel.

* **???** This software is in alpha version. There probably be bugs.