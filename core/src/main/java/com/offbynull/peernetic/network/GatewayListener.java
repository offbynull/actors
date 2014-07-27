package com.offbynull.peernetic.network;

public interface GatewayListener<A> {
    void onReadMessage(Message<A> incomingMessage);
}
