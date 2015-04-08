package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class MessageEvent extends Event {
    final String sourceAddress;
    final String destinationAddress;
    final Object message;

    public MessageEvent(String sourceAddress, String destinationAddress, Object message, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
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
