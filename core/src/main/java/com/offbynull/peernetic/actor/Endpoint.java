package com.offbynull.peernetic.actor;

import java.util.Arrays;
import java.util.Collection;

public interface Endpoint {
    void send(Endpoint source, Object message);
    
    default void send(Endpoint source, Object ... messages) {
        send(source, Arrays.asList(messages));
    }
    
    default void send(Endpoint source, Collection<Object> messages) {
        messages.forEach(x -> send(source, x));
    }
}
