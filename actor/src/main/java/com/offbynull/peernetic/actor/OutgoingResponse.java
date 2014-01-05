package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

public final class OutgoingResponse implements Outgoing {
    private Object id; // id must be non-null
    private Endpoint destination;
    private Object content;

    OutgoingResponse(Object id, Endpoint destination, Object content) {
        Validate.notNull(id);
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
