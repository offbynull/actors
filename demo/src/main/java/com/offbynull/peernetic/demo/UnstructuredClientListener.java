package com.offbynull.peernetic.demo;

public interface UnstructuredClientListener<A> {
    void onStarted(A id);
    void onConnected(A from, A to);
    void onDisconnected(A from, A to);
}
