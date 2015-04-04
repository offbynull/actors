package com.offbynull.peernetic.core.actors.unreliable;

import org.apache.commons.lang3.Validate;

public final class DepartMessage {
    private Object message;
    private String source;
    private String destination;

    public DepartMessage(Object message, String source, String destination) {
        Validate.notNull(message);
        Validate.notNull(source);
        Validate.notNull(destination);
        this.message = message;
        this.source = source;
        this.destination = destination;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
    
}
