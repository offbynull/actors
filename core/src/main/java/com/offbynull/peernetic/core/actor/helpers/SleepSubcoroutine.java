package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class SleepSubcoroutine implements Subcoroutine<Void> {
    private final String sourceId;
    private final String timerAddressPrefix;
    private final Duration timeoutDuration;

    private SleepSubcoroutine(String sourceId, String timerAddressPrefix, Duration timeoutDuration) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(timeoutDuration);
        Validate.isTrue(!timeoutDuration.isNegative());
        this.sourceId = sourceId;
        this.timerAddressPrefix = timerAddressPrefix;
        this.timeoutDuration = timeoutDuration;
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Object timeoutMarker = new Object();
        
        ctx.addOutgoingMessage(
                sourceId,
                AddressUtils.parentize(timerAddressPrefix, "" + timeoutDuration.toMillis()),
                timeoutMarker);
        
        Object incomingMessage;
        do {
            cnt.suspend();
            incomingMessage = ctx.getIncomingMessage();
        } while (incomingMessage != timeoutMarker);
        
        return null;
    }

    @Override
    public String getSourceId() {
        return sourceId;
    }
    
    public static final class Builder {
        private String sourceId;
        private String timerAddressPrefix;
        private Duration timeoutDuration;

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder timerAddressPrefix(String timerAddressPrefix) {
            this.timerAddressPrefix = timerAddressPrefix;
            return this;
        }

        public Builder timeoutDuration(Duration timeoutDuration) {
            this.timeoutDuration = timeoutDuration;
            return this;
        }
        
        public SleepSubcoroutine build() {
            try {
                return new SleepSubcoroutine(sourceId, timerAddressPrefix, timeoutDuration);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalStateException(e);
            }
        }
        
    }
    
}
