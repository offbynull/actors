package com.offbynull.peernetic.debug.actornetwork.messages;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

public final class DepartMessage<A> {
    private Object data;
    private A source;
    private A destination;

    public DepartMessage(Object data, A source, A destination) {
        Validate.notNull(data);
        Validate.notNull(source);
        Validate.notNull(destination);
        this.data = data;
        this.source = source;
        this.destination = destination;
    }

    public Object getData() {
        return data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
    }

    public A getSource() {
        return source;
    }

    public void setSource(A source) {
        this.source = source;
    }

    public A getDestination() {
        return destination;
    }

    public void setDestination(A destination) {
        this.destination = destination;
    }
    
}
