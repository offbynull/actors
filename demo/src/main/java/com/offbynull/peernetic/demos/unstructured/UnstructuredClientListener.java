package com.offbynull.peernetic.demos.unstructured;

public interface UnstructuredClientListener<A> {
    void onStarted(A id);
    void onOutgoingConnected(A from, A to);
    void onDisconnected(A from, A to);
}
