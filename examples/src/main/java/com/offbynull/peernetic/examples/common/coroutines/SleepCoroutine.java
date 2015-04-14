package com.offbynull.peernetic.examples.common.coroutines;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public final class SleepCoroutine implements Coroutine {
    private final String timerAddressPrefix;
    private final Duration timeoutDuration;

    public SleepCoroutine(String timerAddressPrefix, Duration timeoutDuration) {
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(timeoutDuration);
        Validate.isTrue(!timeoutDuration.isNegative());
        this.timerAddressPrefix = timerAddressPrefix;
        this.timeoutDuration = timeoutDuration;
    }
    
    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Object timeoutMarker = new Object();
        
        ctx.addOutgoingMessage(timerAddressPrefix + ":" + timeoutDuration.toMillis(), timeoutMarker);
        cnt.suspend();
        Object incomingMessage = ctx.getIncomingMessage();
        
        if (incomingMessage == timeoutMarker) {
            return;
        }

        throw new IllegalStateException("Unexpected message");
    }
}
