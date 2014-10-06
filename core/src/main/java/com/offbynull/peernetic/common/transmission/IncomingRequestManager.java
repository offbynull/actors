package com.offbynull.peernetic.common.transmission;

import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.Processable;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.Validator;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IncomingRequestManager<A, N> implements Processable {
    
    private static final Logger LOG = LoggerFactory.getLogger(IncomingRequestManager.class);
    
    private final Endpoint selfEndpoint;
    private final PriorityQueue<Event> queue;
    private final NonceAccessor<N> nonceAccessor;
    private final Map<Nonce<N>, Request> requests;
    private final Set<Nonce<N>> removedNonces;
    
    public IncomingRequestManager(Endpoint selfEndpoint, NonceAccessor<N> nonceAccessor) {
        Validate.notNull(selfEndpoint);

        this.selfEndpoint = selfEndpoint;
        this.nonceAccessor = nonceAccessor;
        this.queue = new PriorityQueue<>(new EventTriggerTimeComparator());
        this.requests = new HashMap<>();
        this.removedNonces = new HashSet<>();
    }

    public void track(Instant time, Object request, Endpoint srcEndpoint, Duration trackDuration) {
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(srcEndpoint);
        Validate.notNull(trackDuration);

        Validate.isTrue(!trackDuration.isNegative());

        // generate nonce and validate
        Nonce<N> nonce = nonceAccessor.get(request);

        if (requests.putIfAbsent(nonce, new Request(srcEndpoint, request, null)) != null) {
            LOG.warn("Duplicate tracked request encountered: {}", request);
            return;
        }
        queue.add(new DiscardEvent(nonce, time.plus(trackDuration)));
    }

    public void respond(Instant time, Object request, Object response, Endpoint srcEndpoint) {
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(response);
        Validate.notNull(srcEndpoint);
        
        // generate nonce and validate
        Nonce<N> nonce = nonceAccessor.get(request);
        Request internalReq;
        if ((internalReq = requests.get(nonce)) == null) {
            // may be because respond() wasn't called quickly enough after track() or because the request was not tracked to begin with
            LOG.warn("Request not tracked: {}", request);
            return;
        }
        
        Validate.isTrue(internalReq.getResponse() == null, "Response already sent for request %s", request);
        
        internalReq.setResponse(response);
        
        nonceAccessor.set(response, nonce);
        srcEndpoint.send(selfEndpoint, response);
    }

    @Override
    public Duration process(Instant time) {
        Validate.notNull(time);

        removedNonces.clear();
        
        Iterator<Event> queueIt = queue.iterator();
        while (queueIt.hasNext()) {
            Event event = queueIt.next();

            Instant hitTime = event.getTriggerTime();
            if (hitTime.isAfter(time)) {
                break;
            }

            if (event instanceof IncomingRequestManager.DiscardEvent) {
                DiscardEvent discardEvent = (DiscardEvent) event;
                Nonce<N> nonce = discardEvent.getRequestNonce();
                requests.remove(nonce);
                removedNonces.add(nonce);
            } else {
                throw new IllegalStateException();
            }

            queueIt.remove();
        }
        
        return queue.isEmpty() ? null : Duration.between(time, queue.peek().getTriggerTime());
    }
    
    public Set<Nonce<N>> getRemovedNonces() {
        return new HashSet<>(removedNonces);
    }
    
    public boolean testRequestMessage(Instant time, Object request) {
        Validate.notNull(time);
        Validate.notNull(request);

        // validate response
        if (!Validator.validate(request)) {
            return false; // message failed to validate
        }
        
        // get nonce from response
        Nonce<N> nonce = nonceAccessor.get(request);
        
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

    public boolean isHandledRequest(Instant time, Object message) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(message);

        // validate response
        if (!Validator.validate(message)) {
            return false; // message failed to validate, treat it as if it isn't tracked
        }
        
        // get nonce from response
        Nonce<N> nonce = nonceAccessor.get(message);
        Request requestObj = requests.get(nonce);
        
        return requestObj == null ? false : message.equals(requestObj.getRequest());
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

        public void setResponse(Object response) {
            this.response = response;
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
