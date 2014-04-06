package com.offbynull.peernetic.nettyhelper.handlers.readwrite;

public interface IncomingMessageListener {
    void newMessage(Message incomingMessage);
}
