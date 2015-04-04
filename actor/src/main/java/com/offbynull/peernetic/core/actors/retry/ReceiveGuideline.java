package com.offbynull.peernetic.core.actors.retry;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class ReceiveGuideline {
    private final Duration cacheWaitDuration;

    public ReceiveGuideline(Duration cacheWaitDuration) {
        Validate.notNull(cacheWaitDuration);
        Validate.isTrue(!cacheWaitDuration.isNegative());
        this.cacheWaitDuration = cacheWaitDuration;
    }

    public Duration getCacheWaitDuration() {
        return cacheWaitDuration;
    }
}
