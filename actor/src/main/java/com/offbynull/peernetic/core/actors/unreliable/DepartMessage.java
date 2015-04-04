package com.offbynull.peernetic.core.actors.unreliable;

import org.apache.commons.lang3.Validate;

public final class DepartMessage {
    private final Object message;
    private final String sourceId;
    private final String destinationAddress;

    public DepartMessage(Object message, String sourceId, String destinationAddress) {
        Validate.notNull(message);
        // sourceId can be null
        Validate.notNull(destinationAddress);
        this.message = message;
        this.sourceId = sourceId;
        this.destinationAddress = destinationAddress;
    }

    public Object getMessage() {
        return message;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    
}
