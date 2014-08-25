package com.offbynull.peernetic.playground.unstructuredmesh;

public interface UnstructuredClientListener<A> {
    void onStarted(A id);
    void onOutgoingConnected(A from, A to);
    void onDisconnected(A from, A to);
}
