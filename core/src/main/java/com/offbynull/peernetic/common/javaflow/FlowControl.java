package com.offbynull.peernetic.common.javaflow;

import com.offbynull.peernetic.JavaflowActor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.transmission.Router;
import com.offbynull.peernetic.javaflow.TaskState;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public final class FlowControl<A, N> {
    private final TaskState state;
    
    private JavaflowActor actor;
    private final Router<A, N> router;
    private final Endpoint selfEndpoint;
    private final EndpointScheduler endpointScheduler;
    private final NonceAccessor<N> nonceAccessor;
    
    private int nextTimerMarker;
    
    
    public FlowControl(TaskState state, Router<A, N> router, Endpoint selfEndpoint, EndpointScheduler endpointScheduler,
            NonceAccessor<N> nonceAccessor) {
        Validate.notNull(state);
        Validate.notNull(router);
        Validate.notNull(selfEndpoint);
        Validate.notNull(endpointScheduler);
        Validate.notNull(nonceAccessor);
        
        this.state = state;
        this.router = router;
        this.selfEndpoint = selfEndpoint;
        this.endpointScheduler = endpointScheduler;
        this.nonceAccessor = nonceAccessor;
    }

    public void initialize(JavaflowActor actor) {
        Validate.notNull(actor);
        this.actor = actor;
    }

    public void waitUntilFinished(JavaflowActor actor, Duration checkInterval) {
        Validate.notNull(actor);
        Validate.notNull(checkInterval);
        Validate.isTrue(checkInterval.compareTo(Duration.ZERO) > 0);
        
        if (actor.isFinished()) {
            return;
        }
        
        startTimerEvent(checkInterval);

        while (true) {
            Continuation.suspend();
            
            // handle timer
            if (isTimerEventHit()) {
                if (actor.isFinished()) {
                    return;
                }
                startTimerEvent(checkInterval);
            }
        }
    }

    public Object waitForRequest(Class<?>... types) {
        Validate.noNullElements(types);
        Validate.isTrue(Arrays.stream(types).allMatch(x -> nonceAccessor.containsNonceField((Class<?>) x)));
        
        try {
            Arrays.stream(types).forEach(x -> { router.addTypeHandler(actor, x); });
            Set<Class<?>> typesSet = new HashSet<>(Arrays.asList(types));

            while (true) {
                Continuation.suspend();

                // return if this is an expected response type
                if (typesSet.contains(state.getMessage().getClass())) {
                    return state.getMessage();
                }
            }
        } finally {
            Arrays.stream(types).forEach(x -> { router.removeTypeHandler(actor, x); });
        }
    }

    public <T> T sendRequestAndWait(Object request, A address, Class<T> expectedResponseType, Duration resendDuration, int maxResendCount,
            Duration retainDuration) {
        sendRequest(request, address, resendDuration, maxResendCount, retainDuration);
        return waitUntilNonce(request, expectedResponseType, retainDuration);
    }

    public void sendRequest(Object request, A address, Duration resendDuration, int maxResendCount, Duration retainDuration) {
        Validate.notNull(request);
        Validate.notNull(address);
        Validate.isTrue(nonceAccessor.containsNonceField(request));
        Validate.notNull(resendDuration);
        Validate.notNull(retainDuration);
        Validate.isTrue(!resendDuration.isNegative() && !retainDuration.isNegative() && maxResendCount >= 0);
        Validate.isTrue(resendDuration.multipliedBy(maxResendCount).compareTo(retainDuration) <= 0);

        router.sendRequest(actor, state.getTime(), request, address, resendDuration, maxResendCount, retainDuration);
    }

    private Object waitUntilNonce(Duration timeout, BooleanSupplier messagePredicate) {
        Validate.notNull(timeout);
        Validate.notNull(messagePredicate);
        Validate.isTrue(timeout.compareTo(Duration.ZERO) > 0);
        
        startTimerEvent(timeout);

        while (true) {
            Continuation.suspend();

            // handle timer
            if (isTimerEventHit()) {
                return null;
            }

            // return if this is an expected response type
            if (!nonceAccessor.containsNonceField(state.getMessage())) {
                continue;
            }

            if (messagePredicate.getAsBoolean()) {
                return state.getMessage();
            }
        }
    }
    
    public <T> T waitUntilNonce(Nonce<N> nonce, Duration timeout) {
        Validate.notNull(nonce);
        
        return (T) waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(state.getMessage());
            return extractedNonce.equals(nonce);
        });
    }

    public <T> T waitUntilNonce(Nonce<N> nonce, Class<?> expectedResponseType, Duration timeout) {
        Validate.notNull(expectedResponseType);
        Validate.notNull(nonce);
        
        return (T) waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(state.getMessage());
            return extractedNonce.equals(nonce) && ClassUtils.isAssignable(state.getMessage().getClass(), expectedResponseType);
        });
    }

    public Object waitUntilNonce(Object request, Duration timeout) {
        Validate.notNull(request);
        Validate.isTrue(nonceAccessor.containsNonceField(request));
        
        Nonce<N> nonce = nonceAccessor.get(request);
        
        return waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(state.getMessage());
            return extractedNonce.equals(nonce);
        });
    }

    public <T> T waitUntilNonce(Object request, Class<T> expectedResponseType, Duration timeout) {
        Validate.notNull(request);
        Validate.notNull(expectedResponseType);
        Validate.isTrue(nonceAccessor.containsNonceField(request));
        
        Nonce<N> nonce = nonceAccessor.get(request);
        
        return (T) waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(state.getMessage());
            return extractedNonce.equals(nonce) && ClassUtils.isAssignable(state.getMessage().getClass(), expectedResponseType);
        });
    }
    
    public void wait(Duration timeout) {
        Validate.notNull(timeout);
        Validate.isTrue(timeout.compareTo(Duration.ZERO) > 0);
        
        startTimerEvent(timeout);
        
        while (true) {
            Continuation.suspend();

            // handle timer
            if (isTimerEventHit()) {
                return;
            }
        }
    }

    private void startTimerEvent(Duration duration) {
        Validate.isTrue(duration.compareTo(Duration.ZERO) > 0);
        
        router.addTypeHandler(actor, TimerTrigger.class);
        endpointScheduler.scheduleMessage(duration, selfEndpoint, selfEndpoint, new TimerTrigger(nextTimerMarker));
        
        nextTimerMarker++;
    }
    

    private boolean isTimerEventHit() {
        if (!(state.getMessage() instanceof FlowControl.TimerTrigger)) {
            return false;
        }
        
        TimerTrigger timerTrigger = (TimerTrigger) state.getMessage();
        return timerTrigger.check(this, nextTimerMarker - 1); 
    }

    private final class TimerTrigger {
        
        private final int timerMarker;

        private TimerTrigger(int timerMarker) {
            this.timerMarker = timerMarker;
        }

        private boolean check(Object parent, int timerMarker) {
            return FlowControl.this == parent && this.timerMarker == timerMarker;
        }
    }
}
