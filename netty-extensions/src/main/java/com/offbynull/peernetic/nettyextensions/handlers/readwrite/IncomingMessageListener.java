package com.offbynull.peernetic.nettyextensions.handlers.readwrite;

public interface IncomingMessageListener {
    void newMessage(Message incomingMessage);
}
