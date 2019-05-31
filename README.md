# JedisPubSubManager

Publish&Subscription Manager for Jedis library. 

By Tom Ansill

## Motivation

The current PubSub implementation in Jedis library is not quite easy to use it requires you to implement custom class and its `subscribe(String... channel)` method blocks so it requires you to create new thread to let that blocking method to continue as you use it. Also, Jedis' `unsubscribe()` method is bit buggy and will break things. 
I needed an easy-to-use library that wraps Jedis's PubSub implementation and let me to use PubSub functionality without any headaches.

## How to use


### Initialization

It's fairly simple to initialize `JedisPubSubManager`. You just call its constructor to start the manager like this:

```java
JedisPubSubManager manager = new JedisPubSUbManager("localhost", 6379);

// Stuff

manager.close(); // Don't forget to close when you're done with it
```

`JedisPubSubManager` is an `AutoCloseable` resource so it's recommended that you use try-with-resources construct to clean up the manager after you're done with it.

### Subscription

Then you can start subscribing to channels.
You subscribe channel with channel name and your `Consumer` function that will be run when someone has published a message on the channel.
When you subscribe to a channel, you will get `Subscription` object in return. 
You need that `Subscription` object to cancel (unsubscribe) your subscription later. 

```java
try(JedisPubSubManager manager = new JedisPubSUbManager("localhost", 6379)){
    
    Subscription subscription = manager.subscribe("my_channel", message -> {
       
        if(message.equals("hello!")) System.out.println("Somebody says hello!");
        
        else System.out.println("Somebody said '" + message + "'");
        
    });
    
    // Do things like publishing or whatever
    
    subscription.cancel(); // Don't forget to unsubscribe by calling cancel()
    
}
```

`Subscription` is also an `AutoCloseable` resource so you can try it with try-with-resources.

You can put as many subscriptions on single channel as much as you want.

## Example

** TODO ** See JUnit tests for examples

## Download

**No Maven Repository available yet ):**

For now, you need to build and install it on your machine.

```bash
git clone https://github.com/tomansill/jedispubsub
cd jedispubsub
mvn install
```

## Known issues

* Doesn't currently support cluster configuration. *However, it is trivial to implement this*

* Limit of `Integer.MAX_VALUE` `Consumers` in a **single** channel.

* ??? This software is in alpha version. There probably be bugs.