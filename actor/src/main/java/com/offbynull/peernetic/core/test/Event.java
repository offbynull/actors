package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

abstract class Event implements Comparable<Event> {
    private final Instant when;

    public Event(Instant when) {
        Validate.notNull(when);
        this.when = when;
    }

    public Instant getWhen() {
        return when;
    }

    @Override
    public int compareTo(Event o) {
        return when.compareTo(o.when); // smallest time to largest time
    }
    
}
