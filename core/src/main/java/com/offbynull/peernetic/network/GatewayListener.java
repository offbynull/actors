package com.offbynull.peernetic.network;

public interface GatewayListener {
    void onReadMessage(Message incomingMessage);
}
