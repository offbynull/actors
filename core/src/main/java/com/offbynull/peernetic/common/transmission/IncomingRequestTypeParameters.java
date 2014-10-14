package com.offbynull.peernetic.common.transmission;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class IncomingRequestTypeParameters {
    private final Duration retainDuration; // max amount of time to ignore duplicates

    public IncomingRequestTypeParameters(Duration retainDuration) {
        Validate.notNull(retainDuration);
        Validate.isTrue(!retainDuration.isNegative());
        this.retainDuration = retainDuration;
    }

    public Duration getRetainDuration() {
        return retainDuration;
    }

}
