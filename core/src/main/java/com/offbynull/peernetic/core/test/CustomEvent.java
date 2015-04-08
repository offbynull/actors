package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class CustomEvent extends Event {
    final Runnable runnable;

    public CustomEvent(Runnable runnable, Instant triggerTime, long sequenceNumber) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(runnable);
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return runnable;
    }
    
}
