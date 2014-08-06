package com.offbynull.peernetic.common;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.commons.lang3.Validate;

public final class OutgoingRequestManager<A, N> {

    private static final Duration DEFAULT_RESEND_DURATION = Duration.ofSeconds(5L);
    private static final int DEFAULT_MAXIMUM_RESEND_ATTEMPTS = 3;
    private static final Duration DEFAULT_RETAIN_DURATION = Duration.ofSeconds(30L);
    
    private final Endpoint selfEndpoint;
    private final NonceGenerator<N> nonceGenerator;
    private final NonceWrapper<N> nonceWrapper;
    private final EndpointDirectory<A> endpointDirectory;

    private final PriorityQueue<Event> queue;
    private final Map<Nonce<N>, Request> requests;
    
    private final Duration defaultResendDuration;
    private final int defaultMaxResendCount;
    private final Duration defaultRetainDuration;


    public OutgoingRequestManager(Endpoint selfEndpoint, NonceGenerator<N> nonceGenerator, NonceWrapper<N> nonceWrapper,
            EndpointDirectory<A> endpointDirectory) {
        this(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory, DEFAULT_RESEND_DURATION, DEFAULT_MAXIMUM_RESEND_ATTEMPTS,
                DEFAULT_RETAIN_DURATION);
    }
    
    public OutgoingRequestManager(Endpoint selfEndpoint, NonceGenerator<N> nonceGenerator, NonceWrapper<N> nonceWrapper,
            EndpointDirectory<A> endpointDirectory, Duration defaultResendDuration, int defaultMaxResendCount,
            Duration defaultRetainDuration) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceWrapper);
        Validate.notNull(nonceGenerator);
        Validate.notNull(endpointDirectory);
        Validate.notNull(defaultResendDuration);
        Validate.notNull(defaultRetainDuration);
        
        Validate.isTrue(!defaultResendDuration.isNegative() && !defaultRetainDuration.isNegative() && defaultMaxResendCount >= 0);
        Validate.isTrue(defaultResendDuration.multipliedBy(defaultMaxResendCount).compareTo(defaultRetainDuration) <= 0);
        
        this.selfEndpoint = selfEndpoint;
        this.nonceWrapper = nonceWrapper;
        this.nonceGenerator = nonceGenerator;
        this.endpointDirectory = endpointDirectory;

        this.queue = new PriorityQueue<>(new EventTriggerTimeComparator());
        this.requests = new HashMap<>();
        
        this.defaultResendDuration = defaultResendDuration;
        this.defaultMaxResendCount = defaultMaxResendCount;
        this.defaultRetainDuration = defaultRetainDuration;
    }

    public void sendRequestAndTrack(Instant time, Object request, A dstAddress) throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        sendRequestAndTrack(time, request, dstAddress, defaultResendDuration, defaultMaxResendCount, defaultRetainDuration);
    }
    
    public void sendRequestAndTrack(Instant time, Object request, A dstAddress, Duration resendDuration, int maxResendCount,
            Duration retainDuration) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(dstAddress);
        Validate.notNull(resendDuration);
        Validate.notNull(retainDuration);
        //Validate.notNull(responseHandler); // can be null

        Validate.isTrue(!resendDuration.isNegative() && !retainDuration.isNegative() && maxResendCount >= 0);
        Validate.isTrue(resendDuration.multipliedBy(maxResendCount).compareTo(retainDuration) <= 0);

        // generate nonce and validate
        Nonce<N> nonce;
        int attempt = 0;
        do {
            Validate.isTrue(attempt < 10, "Unable to generate unique nonce -- likely due to poor nonce generation algorithm");
            attempt++;

            // generate and set a nonce
            N nonceValue = InternalUtils.generateAndSetNonce(nonceGenerator, request);
            nonce = nonceWrapper.wrap(nonceValue);

            // validate before adding to queue and sending
            if (InternalUtils.validateMessage(request)) {
                break;
            }
        } while (requests.containsKey(nonce));

        // sendRequestAndTrack and queue resends/discards
        Endpoint dstEndpoint = endpointDirectory.lookup(dstAddress);
        requests.put(nonce, new Request(dstEndpoint, request));
        for (int i = 0; i < maxResendCount; i++) {
            queue.add(new SendEvent(nonce, time.plus(resendDuration.multipliedBy(i))));
        }
        queue.add(new DiscardEvent(nonce, time.plus(retainDuration)));
        
        dstEndpoint.send(selfEndpoint, request);
    }

    public Instant process(Instant time) {
        return handleQueue(time);
    }
    
    public boolean isRequestTracked(Instant time, Object request) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(request);

        // validate response
        if (!InternalUtils.validateMessage(request)) {
            return false; // message failed to validate, treat it as if it isn't tracked
        }
        
        // get nonce from response
        N nonceValue = InternalUtils.getNonceValue(request);
        Nonce<N> nonce = nonceWrapper.wrap(nonceValue);
        return requests.containsKey(nonce);
    }
    
    public boolean testResponseMessage(Instant time, Object response) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(response);

        // validate response
        if (!InternalUtils.validateMessage(response)) {
            return false; // message failed to validate
        }
        
        // get nonce from response
        N nonceValue = InternalUtils.getNonceValue(response);
        Nonce<N> nonce = nonceWrapper.wrap(nonceValue);
        
        Request request;
        if ((request = requests.get(nonce)) == null) {
            return false; // tell to ignore if nonce not being tracked
        }
        
        if (request.isResponseHandled()) {
            return false; // tell to ignore if response already arrived
        }
        
        request.setResponseHandled(true);
        return true; // tell to handle
    }
    
    private Instant handleQueue(Instant time) {
        while (!queue.isEmpty()) {
            Event event = queue.peek();

            Instant hitTime = event.getTriggerTime();
            if (hitTime.isAfter(time)) {
                break;
            }

            if (event instanceof OutgoingRequestManager.DiscardEvent) {
                DiscardEvent discardEvent = (DiscardEvent) event;
                Nonce<N> nonce = discardEvent.getRequestNonce();
                requests.remove(nonce);
            } else if (event instanceof OutgoingRequestManager.SendEvent) {
                SendEvent sendEvent = (SendEvent) event;
                Nonce<N> nonce = sendEvent.getRequestNonce();
                Request request = requests.get(nonce);
                
                if (!request.isResponseHandled()) { // if no response came in yet, resend the request, otherwise ignore
                    request.getDestination().send(selfEndpoint, request.getRequest()); // request should never be null
                }
            } else {
                throw new IllegalStateException();
            }

            queue.poll(); // remove
        }
        
        return queue.isEmpty() ? null : queue.peek().getTriggerTime();
    }

    private static final class Request {

        private Endpoint destination;
        private Object request;
        private boolean responseHandled;

        public Request(Endpoint destination, Object request) {
            this.destination = destination;
            this.request = request;
        }

        public Endpoint getDestination() {
            return destination;
        }

        public Object getRequest() {
            return request;
        }

        public boolean isResponseHandled() {
            return responseHandled;
        }

        public void setResponseHandled(boolean responseHandled) {
            this.responseHandled = responseHandled;
        }
    }

    private abstract class Event {

        private final Instant triggerTime;

        public Event(Instant triggerTime) {
            this.triggerTime = triggerTime;
        }

        public Instant getTriggerTime() {
            return triggerTime;
        }

    }

    private final class EventTriggerTimeComparator implements Comparator<Event> {

        @Override
        public int compare(Event o1, Event o2) {
            return o1.getTriggerTime().compareTo(o2.getTriggerTime());
        }

    }

    private final class SendEvent extends Event {

        private final Nonce<N> requestNonce;

        public SendEvent(Nonce<N> requestNonce, Instant triggerTime) {
            super(triggerTime);
            this.requestNonce = requestNonce;
        }

        public Nonce<N> getRequestNonce() {
            return requestNonce;
        }
    }

    private final class DiscardEvent extends Event {

        private final Nonce<N> requestNonce;

        public DiscardEvent(Nonce<N> requestNonce, Instant triggerTime) {
            super(triggerTime);
            this.requestNonce = requestNonce;
        }

        public Nonce<N> getRequestNonce() {
            return requestNonce;
        }
    }
}
