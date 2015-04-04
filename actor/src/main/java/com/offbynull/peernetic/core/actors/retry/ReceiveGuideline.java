package com.offbynull.peernetic.core.actors.retry;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class ReceiveGuideline {
    private final Duration waitDuration;

    public ReceiveGuideline(Duration waitDuration) {
        Validate.notNull(waitDuration);
        Validate.isTrue(!waitDuration.isNegative());
        this.waitDuration = waitDuration;
    }

    public Duration getWaitDuration() {
        return waitDuration;
    }
}
