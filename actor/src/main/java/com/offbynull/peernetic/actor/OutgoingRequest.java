package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

public final class OutgoingRequest implements Outgoing {
    private Object id; // null id means it doesn't expect a response
    private Endpoint destination;
    private Object content;

    OutgoingRequest(Object id, Endpoint destination, Object content) {
        Validate.notNull(destination);
        Validate.notNull(content);
        
        this.id = id;
        this.destination = destination;
        this.content = content;
    }

    Object getId() {
        return id;
    }

    public Endpoint getDestination() {
        return destination;
    }

    public Object getContent() {
        return content;
    }
    
}
