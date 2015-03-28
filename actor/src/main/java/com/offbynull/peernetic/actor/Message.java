package com.offbynull.peernetic.actor;

import org.apache.commons.lang3.Validate;

public final class Message {
    private final String sourceAddress;
    private final String destinationAddress;
    private final Object message;

    public Message(String sourceAddress, String destinationAddress, Object message) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notEmpty(sourceAddress);
        Validate.notEmpty(destinationAddress);
        
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.message = message;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public Object getMessage() {
        return message;
    }

    
}
