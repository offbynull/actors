package com.offbynull.peernetic.networktools.netty.handlers.readwrite;

public interface IncomingMessageListener {
    void newMessage(Message incomingMessage);
}
