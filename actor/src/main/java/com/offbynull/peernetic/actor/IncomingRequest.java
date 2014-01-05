package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

public final class IncomingRequest implements Incoming {
    private Object id; // if null id, no response possible.
    private Endpoint source;
    private Object content;

    IncomingRequest(Object id, Endpoint source, Object content) {
        Validate.notNull(source);
        Validate.notNull(content);
        
        this.id = id;
        this.source = source;
        this.content = content;
    }

    Object getId() {
        return id;
    }

    public Endpoint getSource() {
        return source;
    }

    public Object getContent() {
        return content;
    }

}
