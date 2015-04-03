package com.offbynull.peernetic.core.actors.reliableproxy;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class ReceiveGuideline {
    private final String messageId;
    private final Duration cacheWaitDuration;

    public ReceiveGuideline(String messageId, Duration cacheWaitDuration) {
        Validate.notNull(messageId);
        Validate.notNull(cacheWaitDuration);
        Validate.isTrue(!cacheWaitDuration.isNegative());
        this.cacheWaitDuration = cacheWaitDuration;
        this.messageId = messageId;
    }

    public Duration getCacheWaitDuration() {
        return cacheWaitDuration;
    }

    public String getMessageId() {
        return messageId;
    }
    
}
