package com.offbynull.peernetic.network;

public interface Gateway<A> extends AutoCloseable {
//    void initialize(IncomingMessageListener listener, A ... sources);
    void send(A destination, Object message);
}
