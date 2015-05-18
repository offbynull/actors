/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.simulator;

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
    public String toString() {
        return "Event{" + "triggerTime=" + triggerTime + ", seqNum=" + seqNum + '}';
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
