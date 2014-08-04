package com.offbynull.peernetic.common;

import com.offbynull.peernetic.actor.Endpoint;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.commons.lang3.Validate;

public final class IncomingRequestManager<A, N> {

    private final Endpoint selfEndpoint;

    private final PriorityQueue<Event> queue;
    private final NonceWrapper<N> nonceWrapper;
    private final Map<Nonce<N>, Request> requests;


    public IncomingRequestManager(Endpoint selfEndpoint, NonceWrapper<N> nonceWrapper) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceWrapper);

        this.selfEndpoint = selfEndpoint;
        this.nonceWrapper = nonceWrapper;

        this.queue = new PriorityQueue<>(new EventTriggerTimeComparator());
        this.requests = new HashMap<>();
    }

    public void sendResponseAndTrack(Instant time, Object request, Object response, Endpoint srcEndpoint, Duration retainDuration)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(request);
        // Validate.notNull(response); // can be null
        Validate.notNull(srcEndpoint);
        Validate.notNull(retainDuration);

        Validate.isTrue(!retainDuration.isNegative());

        // generate nonce and validate
        N nonceValue = InternalUtils.getNonceValue(request);
        Nonce<N> nonce = nonceWrapper.wrap(nonceValue);

        Validate.isTrue(!requests.containsKey(nonce), "Duplicate stored nonce encountered");
        
        InternalUtils.setNonceValue(response, nonceValue);
        
        // send and queue resends/discards
        requests.put(nonce, new Request(srcEndpoint, request, response));
        queue.add(new DiscardEvent(nonceValue, time.plus(retainDuration)));
        
        srcEndpoint.send(selfEndpoint, response);
    }

    public Instant process(Instant time) {
        return handleQueue(time);
    }
    
    public boolean testRequestMessage(Instant time, Object request) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(request);

        // validate response
        if (!InternalUtils.validateMessage(request)) {
            return false; // message failed to validate
        }
        
        // get nonce from response
        N nonceValue = InternalUtils.getNonceValue(request);
        Nonce<N> nonce = nonceWrapper.wrap(nonceValue);
        
        Request requestObj = requests.get(nonce);
        
        if (requestObj == null) {
            return true; // first time seeing this request, tell to handle
        }
        
        if (requestObj.getResponse() != null) {
            requestObj.getSource().send(selfEndpoint, requestObj.getResponse()); // if responded to already, respond with stored response
            return false;
        }
        
        return false; // request already received, but hasn't been responded to yet
    }
    
    private Instant handleQueue(Instant time) {
        Iterator<Event> queueIt = queue.iterator();
        while (queueIt.hasNext()) {
            Event event = queueIt.next();

            Instant hitTime = event.getTriggerTime();
            if (hitTime.isAfter(time)) {
                break;
            }

            if (event instanceof IncomingRequestManager.DiscardEvent) {
                DiscardEvent discardEvent = (DiscardEvent) event;
                N nonceValue = discardEvent.getRequestNonce();
                Nonce<N> nonce = nonceWrapper.wrap(nonceValue);
                requests.remove(nonce);
            } else {
                throw new IllegalStateException();
            }

            queueIt.remove();
        }
        
        return queue.isEmpty() ? null : queue.peek().getTriggerTime();
    }

    private static final class Request {

        private Endpoint source;
        private Object request;
        private Object response;

        public Request(Endpoint source, Object request, Object response) {
            this.source = source;
            this.request = request;
            this.response = response;
        }

        public Endpoint getSource() {
            return source;
        }

        public Object getRequest() {
            return request;
        }

        public Object getResponse() {
            return response;
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

    private final class DiscardEvent extends Event {

        private final N requestNonce;

        public DiscardEvent(N requestNonce, Instant triggerTime) {
            super(triggerTime);
            this.requestNonce = requestNonce;
        }

        public N getRequestNonce() {
            return requestNonce;
        }
    }
}
