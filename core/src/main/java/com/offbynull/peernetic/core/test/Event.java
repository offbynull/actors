package com.offbynull.peernetic.core.test;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

abstract class Event implements Comparable<Event> {
    private final Instant triggerTime;
    private final long seqNum; // An automatically-incremented sequence number as a secondary key. Used to break ties when
                                       // triggerTimes are equal.
                                       //
                                       // http://stackoverflow.com/questions/15731967/priorityqueue-has-objects-with-the-same-priority

    public Event(Instant triggerTime, long sequenceNumber) {
        Validate.notNull(triggerTime);
        this.triggerTime = triggerTime;
        this.seqNum = sequenceNumber;
    }

    public Instant getTriggerTime() {
        return triggerTime;
    }

    public long getSequenceNumber() {
        return seqNum;
    }

    @Override
    public int compareTo(Event o) {
        if (!triggerTime.equals(o.triggerTime)) {
            return triggerTime.compareTo(o.triggerTime); // smallest time to largest time
        } else {
            return Long.compare(seqNum, o.seqNum);
        }
    }
    
}
