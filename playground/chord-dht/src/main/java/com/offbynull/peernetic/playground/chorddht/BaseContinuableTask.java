package com.offbynull.peernetic.playground.chorddht;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.ProcessableUtils;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.transmission.Router;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public abstract class BaseContinuableTask<A, N> implements ContinuableTask {

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private Instant time;
    private Endpoint source;
    private Object message;
    private ContinuationActor actor;

    private Router<A, N> router;
    private final Endpoint selfEndpoint;
    private final EndpointScheduler endpointScheduler;
    private final NonceAccessor<N> nonceAccessor;

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

    protected void waitUntilFinished(ContinuationActor actor) {
        if (actor.isFinished()) {
            return;
        }

        while (true) {
            Continuation.suspend();
            
            // handle timer
            if (message.getClass() == TimerTrigger.class) {
                handleTimerEvent();
            }

            if (actor.isFinished()) {
                return;
            }
        }
    }

    protected Object waitUntilType(Class<?>... types) {
        Validate.noNullElements(types);
        Set<Class<?>> typesSet = new HashSet<>(Arrays.asList(types));

        while (true) {
            Continuation.suspend();

            // handle timer
            if (message.getClass() == TimerTrigger.class) {
                if (!handleTimerEvent()) {
                    continue;
                }
            }

            // return if this is an expected response type
            if (typesSet.contains(message.getClass())) {
                return message;
            }
        }
    }

    protected void waitUntilResponse(Object request) {
        Validate.notNull(request);
        Validate.isTrue(nonceAccessor.containsNonceField(request));

        Nonce<N> nonce = nonceAccessor.get(request);

        while (true) {
            Continuation.suspend();

            // handle timer
            if (message.getClass() == TimerTrigger.class) {
                if (!handleTimerEvent()) {
                    continue;
                }
            }

            // return if this is an expected response type
            if (!nonceAccessor.containsNonceField(message)) {
                continue;
            }

            Nonce<N> extractedNonce = nonceAccessor.get(message);
            if (extractedNonce.equals(nonce)) {
                return;
            }
        }
    }

    protected void waitUntilNonce(Nonce<N> nonce) {
        Validate.notNull(nonce);

        while (true) {
            Continuation.suspend();

            // handle timer
            if (message.getClass() == TimerTrigger.class) {
                if (!handleTimerEvent()) {
                    continue;
                }
            }

            // return if this is an expected response type
            if (!nonceAccessor.containsNonceField(message)) {
                continue;
            }

            Nonce<N> extractedNonce = nonceAccessor.get(message);
            if (extractedNonce.equals(nonce)) {
                return;
            }
        }
    }

    protected void waitCycles(int count) {
        Validate.isTrue(count > 0);

        
        while (count > 0) {
            Continuation.suspend();

            // handle timer
            if (message.getClass() == TimerTrigger.class) {
                if (handleTimerEvent()) {
                    count--;
                }
            }
        }
    }

    protected <T> T sendAndWaitUntilResponse(Object request, A address, Class<?> expectedResponseType) {
        Validate.notNull(request);
        Validate.isTrue(nonceAccessor.containsNonceField(request));

        router.sendRequest(actor, time, request, address);
        
        Nonce<N> nonce = nonceAccessor.get(request);

        while (true) {
            Continuation.suspend();

            // handle timer
            if (message.getClass() == TimerTrigger.class) {
                if (!handleTimerEvent()) {
                    continue;
                }
            }

            // return if this is an expected response type
            if (!nonceAccessor.containsNonceField(message)) {
                continue;
            }

            Nonce<N> extractedNonce = nonceAccessor.get(message);
            if (extractedNonce.equals(nonce) && ClassUtils.isAssignable(message.getClass(), expectedResponseType)) {
                return (T) message;
            }
        }
    }

    protected void scheduleTimer() {
        router.addTypeHandler(actor, TimerTrigger.class);
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
    }
    

    private boolean handleTimerEvent() {
        TimerTrigger timerTrigger = (TimerTrigger) message;
        if (!timerTrigger.checkParent(this)) {
            // this timertrigger is from some other routetofingertask (probably a prior one), ignore it
            return false;
        }

        if (router.getPendingResponseCount(actor) == 0) {
            // we have no more messages pending, so stop
            Continuation.exit();
            return false; // never makes it to this point, here jsut in case
        }

        Duration nextDuration = ProcessableUtils.invokeProcessablesAndScheduleEarliestDuration(time);
        if (nextDuration == null) {
            nextDuration = TIMER_DURATION;
        }
        endpointScheduler.scheduleMessage(nextDuration, selfEndpoint, selfEndpoint, new TimerTrigger());

        return true; // mark as handled
    }

    private final class TimerTrigger {

        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }

        public boolean checkParent(Object obj) {
            return BaseContinuableTask.this == obj;
        }
    }
}
