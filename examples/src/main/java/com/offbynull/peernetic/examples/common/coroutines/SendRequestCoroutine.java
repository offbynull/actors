package com.offbynull.peernetic.examples.common.coroutines;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public final class SendRequestCoroutine implements Coroutine {
    private final String sourceId;
    private final String destinationAddress;
    private final ExternalMessage request;
    private final String timerAddressPrefix;
    private final Duration timeoutDuration;
    private final Class<? extends ExternalMessage> expectedResponseType;
    private ExternalMessage response;

    public SendRequestCoroutine(String sourceId, String destinationAddress, ExternalMessage request, String timerAddressPrefix,
            Duration timeoutDuration, Class<? extends ExternalMessage> expectedResponseType) {
        Validate.notNull(sourceId);
        Validate.notNull(destinationAddress);
        Validate.notNull(request);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(timeoutDuration);
        Validate.notNull(expectedResponseType);
        Validate.isTrue(!timeoutDuration.isNegative());
        this.sourceId = sourceId;
        this.destinationAddress = destinationAddress;
        this.request = request;
        this.timerAddressPrefix = timerAddressPrefix;
        this.timeoutDuration = timeoutDuration;
        this.expectedResponseType = expectedResponseType;
    }
    
    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Object timeoutMarker = new Object();
        
        ctx.addOutgoingMessage(sourceId, destinationAddress, request);
        ctx.addOutgoingMessage(timerAddressPrefix + ":" + timeoutDuration.toMillis(), timeoutMarker);
        cnt.suspend();
        Object incomingMessage = ctx.getIncomingMessage();
        
        if (incomingMessage == timeoutMarker) {
            throw new IllegalStateException("No response");
        }
        
        if (!ClassUtils.isAssignable(response.getClass(), expectedResponseType)) {
            throw new IllegalStateException("Bad response type");
            
        }
        
        response = (ExternalMessage) incomingMessage;
    }

    @SuppressWarnings("unchecked")
    public <T extends ExternalMessage> T getResponse() {
        return (T) response;
    }
    
}
