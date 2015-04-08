package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

abstract class Event implements Comparable<Event> {
    private final Instant triggerTime;

    public Event(Instant triggerTime) {
        Validate.notNull(triggerTime);
        this.triggerTime = triggerTime;
    }

    public Instant getTriggerTime() {
        return triggerTime;
    }

    @Override
    public int compareTo(Event o) {
        return triggerTime.compareTo(o.triggerTime); // smallest time to largest time
    }
    
}
