package com.offbynull.peernetic.core.test;

import java.time.Instant;

final class PullFromMessageSourceEvent extends Event {

    private final MessageSource messageSource;
    
    public PullFromMessageSourceEvent(MessageSource messageSource, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        this.messageSource = messageSource;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }
    
}
