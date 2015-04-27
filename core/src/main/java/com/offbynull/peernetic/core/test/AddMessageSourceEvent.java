package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class AddMessageSourceEvent extends Event {
    private final MessageSource messageSource;

    public AddMessageSourceEvent(MessageSource messageSource, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(messageSource);
        
        this.messageSource = messageSource;
    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

}
