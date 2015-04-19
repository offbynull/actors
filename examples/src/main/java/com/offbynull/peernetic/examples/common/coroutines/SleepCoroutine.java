package com.offbynull.peernetic.examples.common.coroutines;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

public final class SleepCoroutine implements Coroutine {
    private final String sourceId;
    private final String timerAddressPrefix;
    private final Duration timeoutDuration;

    public SleepCoroutine(String sourceId, String timerAddressPrefix, Duration timeoutDuration) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(timeoutDuration);
        Validate.isTrue(!timeoutDuration.isNegative());
        this.sourceId = sourceId;
        this.timerAddressPrefix = timerAddressPrefix;
        this.timeoutDuration = timeoutDuration;
    }
    
    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Object timeoutMarker = new Object();
        
        ctx.addOutgoingMessage(sourceId, timerAddressPrefix + ":" + timeoutDuration.toMillis(), timeoutMarker);
        
        Object incomingMessage;
        do {
            cnt.suspend();
            incomingMessage = ctx.getIncomingMessage();
        } while (incomingMessage != timeoutMarker);
    }
}
