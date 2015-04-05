package com.offbynull.peernetic.core.test;

import java.util.List;

public interface MessageDriver {
    List<MessageEnvelope> onMessageSend(String sourceAddress, String destinationAddress, Object message);
}
