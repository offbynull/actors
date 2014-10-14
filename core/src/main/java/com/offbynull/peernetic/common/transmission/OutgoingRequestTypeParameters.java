package com.offbynull.peernetic.common.transmission;

import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class OutgoingRequestTypeParameters {
    private final Duration resendDuration;
    private final Duration responseDuration; // max amount of time to wait till response
    private final int maxSendCount;

    public OutgoingRequestTypeParameters(Duration resendDuration, Duration responseDuration, int maxSendCount) {
        Validate.notNull(resendDuration);
        Validate.notNull(responseDuration);
        Validate.isTrue(!resendDuration.isNegative());
        Validate.isTrue(!responseDuration.isNegative());
        Validate.isTrue(maxSendCount > 0);
        Validate.isTrue(resendDuration.multipliedBy(maxSendCount).compareTo(responseDuration) <= 0);
        this.resendDuration = resendDuration;
        this.responseDuration = responseDuration;
        this.maxSendCount = maxSendCount;
    }

    public Duration getResendDuration() {
        return resendDuration;
    }

    public Duration getResponseDuration() {
        return responseDuration;
    }

    public int getMaxSendCount() {
        return maxSendCount;
    }
    
}
