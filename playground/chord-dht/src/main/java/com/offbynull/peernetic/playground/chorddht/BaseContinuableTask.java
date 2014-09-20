package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.transmission.Router;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public abstract class BaseContinuableTask<A, N> implements ContinuableTask {

    private Instant time;
    private Endpoint source;
    private Object message;
    private ContinuationActor actor;

    private Router<A, N> router;
    private final Endpoint selfEndpoint;
    private final EndpointScheduler endpointScheduler;
    private final NonceAccessor<N> nonceAccessor;
    
    private int nextTimerMarker;

    public BaseContinuableTask(Router<A, N> router, Endpoint selfEndpoint, EndpointScheduler endpointScheduler,
            NonceAccessor<N> nonceAccessor) {
        this(selfEndpoint, endpointScheduler, nonceAccessor);
        
        Validate.notNull(router);
        this.router = router;
    }

    public BaseContinuableTask(Endpoint selfEndpoint, EndpointScheduler endpointScheduler, NonceAccessor<N> nonceAccessor) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(endpointScheduler);
        Validate.notNull(nonceAccessor);

        this.selfEndpoint = selfEndpoint;
        this.endpointScheduler = endpointScheduler;
        this.nonceAccessor = nonceAccessor;
    }

    @Override
    public void setTime(Instant time) {
        this.time = time;
    }

    @Override
    public void setSource(Endpoint source) {
        this.source = source;
    }

    @Override
    public void setMessage(Object message) {
        this.message = message;
    }

    public Instant getTime() {
        return time;
    }

    public Endpoint getSource() {
        return source;
    }

    public Object getMessage() {
        return message;
    }

    public void setEncapsulatingActor(ContinuationActor encapsulatingActor) {
        this.actor = encapsulatingActor;
    }

    public void setRouter(Router<A, N> router) {
        this.router = router;
    }

    public ContinuationActor getEncapsulatingActor() {
        return actor;
    }

    public abstract void execute() throws Exception;
    
    @Override
    public final void run() {
        try {
            execute();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            router.removeActor(actor);
        }
    }

    protected void waitUntilFinished(ContinuationActor actor, Duration checkInterval) {
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

    protected Object waitForRequest(Class<?>... types) {
        Validate.noNullElements(types);
        Validate.isTrue(Arrays.stream(types).allMatch(x -> nonceAccessor.containsNonceField((Class<?>) x)));
        Set<Class<?>> typesSet = new HashSet<>(Arrays.asList(types));
        
        while (true) {
            Continuation.suspend();

            // return if this is an expected response type
            if (typesSet.contains(message.getClass())) {
                return message;
            }
        }
    }

    protected <T> T sendRequestAndWait(Object request, A address, Class<T> expectedResponseType, Duration timeout) {
        sendRequest(request, address);
        return waitUntilNonce(request, expectedResponseType, timeout);
    }

    protected void sendRequest(Object request, A address) {
        Validate.notNull(request);
        Validate.isTrue(nonceAccessor.containsNonceField(request));

        router.sendRequest(actor, time, request, address);
    }

    private Object waitUntilNonce(Duration timeout, BooleanSupplier messagePredicate) {
        Validate.notNull(timeout);
        Validate.notNull(messagePredicate);
        Validate.isTrue(timeout.compareTo(Duration.ZERO) >= 0);
        
        startTimerEvent(timeout);

        while (true) {
            Continuation.suspend();

            // handle timer
            if (isTimerEventHit()) {
                return null;
            }

            // return if this is an expected response type
            if (!nonceAccessor.containsNonceField(message)) {
                continue;
            }

            if (messagePredicate.getAsBoolean()) {
                return message;
            }
        }
    }
    
    protected <T> T waitUntilNonce(Nonce<N> nonce, Duration timeout) {
        Validate.notNull(timeout);
        Validate.notNull(nonce);
        Validate.isTrue(timeout.compareTo(Duration.ZERO) >= 0);
        
        return (T) waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(message);
            return extractedNonce.equals(nonce);
        });
    }

    protected <T> T waitUntilNonce(Nonce<N> nonce, Class<?> expectedResponseType, Duration timeout) {
        Validate.notNull(expectedResponseType);
        Validate.notNull(timeout);
        Validate.notNull(nonce);
        Validate.isTrue(timeout.compareTo(Duration.ZERO) >= 0);
        
        return (T) waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(message);
            return extractedNonce.equals(nonce) && ClassUtils.isAssignable(message.getClass(), expectedResponseType);
        });
    }

    protected Object waitUntilNonce(Object request, Duration timeout) {
        Validate.notNull(timeout);
        Validate.notNull(request);
        Validate.isTrue(nonceAccessor.containsNonceField(request));
        Validate.isTrue(timeout.compareTo(Duration.ZERO) >= 0);
        
        Nonce<N> nonce = nonceAccessor.get(request);
        
        return waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(message);
            return extractedNonce.equals(nonce);
        });
    }

    protected <T> T waitUntilNonce(Object request, Class<T> expectedResponseType, Duration timeout) {
        Validate.notNull(expectedResponseType);
        Validate.notNull(timeout);
        Validate.isTrue(nonceAccessor.containsNonceField(request));
        Validate.isTrue(timeout.compareTo(Duration.ZERO) >= 0);
        
        Nonce<N> nonce = nonceAccessor.get(request);
        
        return (T) waitUntilNonce(timeout, () -> {
            Nonce<N> extractedNonce = nonceAccessor.get(message);
            return extractedNonce.equals(nonce) && ClassUtils.isAssignable(message.getClass(), expectedResponseType);
        });
    }
    
    protected void wait(Duration timeout) {
        Validate.notNull(timeout);
        Validate.isTrue(timeout.compareTo(Duration.ZERO) >= 0);
        
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
        router.addTypeHandler(actor, TimerTrigger.class);
        endpointScheduler.scheduleMessage(duration, selfEndpoint, selfEndpoint, new TimerTrigger(nextTimerMarker));
        
        nextTimerMarker++;
    }
    

    private boolean isTimerEventHit() {
        if (!(message instanceof BaseContinuableTask.TimerTrigger)) {
            return false;
        }
        
        TimerTrigger timerTrigger = (TimerTrigger) message;
        return timerTrigger.check(this, nextTimerMarker - 1); 
    }

    private final class TimerTrigger {
        
        private final int timerMarker;

        private TimerTrigger(int timerMarker) {
            this.timerMarker = timerMarker;
        }

        public boolean check(Object parent, int timerMarker) {
            return BaseContinuableTask.this == parent && this.timerMarker == timerMarker;
        }
    }
}
