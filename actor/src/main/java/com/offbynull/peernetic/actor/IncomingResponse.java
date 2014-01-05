package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

public final class IncomingResponse implements Incoming {
    private Object id; // id of original request
    private Endpoint source;
    private Object content;

    IncomingResponse(Object id, Endpoint source, Object content) {
        Validate.notNull(id);
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
