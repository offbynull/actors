package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class CustomEvent extends Event {
    final Runnable runnable;

    public CustomEvent(Runnable runnable, Instant triggerTime) {
        super(triggerTime);
        Validate.notNull(runnable);
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return runnable;
    }
    
}
