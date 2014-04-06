package com.offbynull.peernetic.network.handlers.readwrite;

public interface IncomingMessageListener {
    void newMessage(Message incomingMessage);
}
