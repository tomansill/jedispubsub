package com.ansill.redis;

import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public final class ServerUtility{

    private static final Set<Integer> RESERVED_PORTS = new HashSet<>();

    private ServerUtility(){
    }

    @Nonnull
    public static Server getServer(){

        // Return new instance
        return new Server();
    }

    @SuppressWarnings("SameParameterValue")
    @Nonnegative
    private static synchronized int getNextOpenPort(@Nonnegative int start){

        // Loop until maximum possible
        for(int port = start; port < Short.MAX_VALUE; port++){

            // Make sure not already reserved
            if(RESERVED_PORTS.contains(port)) continue;

            // Check if it's open
            if(!isPortOpen(port)) continue;

            // If it is indeed open, reserve it
            RESERVED_PORTS.add(port);

            // Return it
            return port;
        }

        // Throw it
        throw new RuntimeException("Cannot find any open ports!");
    }

    private static synchronized void unreservePort(@Nonnegative int port){
        RESERVED_PORTS.remove(port);
    }

    private static boolean isPortOpen(@Nonnegative int port){
        try(ServerSocket ignored = new ServerSocket(port)){
            return true;
        }catch(IOException e){
            return false;
        }
    }

    @SuppressWarnings("SameReturnValue")
    public final static class Server implements AutoCloseable{

        @Nonnull
        private final RedisServer server;
        @Nonnegative
        private final int port;
        @Nonnull
        private final Jedis pubsub_conn;
        private boolean running = true;

        private Server(){

            // Find a port
            this.port = getNextOpenPort(6379);

            // Start it up
            try{
                this.server = new RedisServer(port);
                this.server.start();
            }catch(IOException e){
                // Wrap it
                throw new RuntimeException(e);
            }

            // Set up pubsub connection
            this.pubsub_conn = this.getConnection();
        }

        @Nonnull
        public String getHostname(){
            return "localhost";
        }

        public int getPort(){
            return this.port;
        }

        @Nonnull
        public Jedis getConnection(){
            return new Jedis("localhost", this.port);
        }

        @Override
        public void close(){
            if(this.running){
                this.pubsub_conn.close();
                this.server.stop();
                unreservePort(this.port);
            }
            this.running = false;
        }
    }

}
