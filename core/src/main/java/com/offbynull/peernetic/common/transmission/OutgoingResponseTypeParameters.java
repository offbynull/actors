package com.offbynull.peernetic.common.transmission;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class OutgoingResponseTypeParameters {
    private final Duration retainDuration; // max amount of time to hold on to this response, which will automatically be resent if the
                                           // request comes in again

    public OutgoingResponseTypeParameters(Duration retainDuration) {
        Validate.notNull(retainDuration);
        Validate.isTrue(!retainDuration.isNegative());
        this.retainDuration = retainDuration;
    }

    public Duration getRetainDuration() {
        return retainDuration;
    }

}
