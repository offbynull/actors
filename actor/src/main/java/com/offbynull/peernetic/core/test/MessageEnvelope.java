package com.offbynull.peernetic.core.test;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class MessageEnvelope {
    private final Object message;
    private final String sourceAddress;
    private final String destinationAddress;
    private final Duration duration;

    public MessageEnvelope(String sourceAddress, String destinationAddress, Object message, Duration duration) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notNull(duration);
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.message = message;
        this.duration = duration;
    }

    public Object getMessage() {
        return message;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public Duration getDuration() {
        return duration;
    }
    
}
