package com.offbynull.peernetic.core.actors.unreliable;

import org.apache.commons.lang3.Validate;

public final class DepartMessage {
    private final Object message;
    private final String sourceSuffix;
    private final String destinationAddress;

    public DepartMessage(Object message, String sourceSuffix, String destinationAddress) {
        Validate.notNull(message);
        // sourceSuffix can be null
        Validate.notNull(destinationAddress);
        this.message = message;
        this.sourceSuffix = sourceSuffix;
        this.destinationAddress = destinationAddress;
    }

    public Object getMessage() {
        return message;
    }

    public String getSourceSuffix() {
        return sourceSuffix;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    
}
