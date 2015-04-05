package com.offbynull.peernetic.core.test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public final class SimpleMessageDriver implements MessageDriver {

    @Override
    public List<MessageEnvelope> onMessageSend(String sender, String receiver, Object message) {
        return Collections.singletonList(new MessageEnvelope(sender, receiver, message, Duration.ZERO));
    }
    
}
