package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class AddMessageSinkEvent extends Event {
    private final MessageSink messageSink;

    public AddMessageSinkEvent(MessageSink messageSink, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(messageSink);
        
        this.messageSink = messageSink;
    }

    public MessageSink getMessageSink() {
        return messageSink;
    }
}
