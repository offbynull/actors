package com.offbynull.peernetic.core.actors.retry;

import java.time.Duration;
import java.util.Arrays;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class SendGuideline {
    private final UnmodifiableList<Duration> sendDurations;
    private final Duration ackWaitDuration;

    public SendGuideline(Duration ackWaitDuration, Duration ... sendDurations) {
        Validate.notNull(ackWaitDuration);
        Validate.isTrue(!ackWaitDuration.isNegative());
        Validate.notNull(sendDurations);
        Validate.noNullElements(sendDurations);

        this.sendDurations = (UnmodifiableList<Duration>) UnmodifiableList.unmodifiableList(Arrays.asList(sendDurations));
        this.ackWaitDuration = ackWaitDuration;
    }

    public UnmodifiableList<Duration> getSendDurations() {
        return sendDurations;
    }

    public Duration getAckWaitDuration() {
        return ackWaitDuration;
    }
}
