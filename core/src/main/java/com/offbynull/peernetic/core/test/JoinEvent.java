package com.offbynull.peernetic.core.test;

import com.offbynull.peernetic.core.actor.Actor;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

final class JoinEvent extends Event {
    final String address;
    final Actor actor;
    final Duration timeOffset;
    final UnmodifiableList<Object> primingMessages;

    public JoinEvent(String address, Actor actor, Duration timeOffset, Instant triggerTime, long sequenceNumber,
            Object... primingMessages) {
        super(triggerTime, sequenceNumber);
        Validate.notNull(address);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(triggerTime);
        Validate.notNull(primingMessages);
        Validate.isTrue(!timeOffset.isNegative());
        Validate.noNullElements(primingMessages);
        
        this.address = address;
        this.actor = actor;
        this.timeOffset = timeOffset;
        this.primingMessages = (UnmodifiableList<Object>) UnmodifiableList.unmodifiableList(Arrays.asList(primingMessages));
    }

    public String getAddress() {
        return address;
    }

    public Actor getActor() {
        return actor;
    }

    public Duration getTimeOffset() {
        return timeOffset;
    }

    public UnmodifiableList<Object> getPrimingMessages() {
        return primingMessages;
    }
    
}
