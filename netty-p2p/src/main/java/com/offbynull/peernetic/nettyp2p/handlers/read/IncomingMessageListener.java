package com.offbynull.peernetic.nettyp2p.handlers.read;

public interface IncomingMessageListener {
    void newMessage(IncomingMessage incomingMessage);
}
