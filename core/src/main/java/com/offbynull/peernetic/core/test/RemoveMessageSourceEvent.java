package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class RemoveMessageSourceEvent extends Event {
    private final MessageSource messageSource;

    public RemoveMessageSourceEvent(MessageSource messageSource, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(messageSource);
        
        this.messageSource = messageSource;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

}
