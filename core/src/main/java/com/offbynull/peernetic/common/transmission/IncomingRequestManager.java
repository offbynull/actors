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

public final class IncomingRequestManager<A, N> implements Processable {

    private static final Duration DEFAULT_RETAIN_DURATION = Duration.ofSeconds(60L);
    
    private final Endpoint selfEndpoint;

    private final PriorityQueue<Event> queue;
    private final NonceAccessor<N> nonceAccessor;
    private final Map<Nonce<N>, Request> requests;

    private final Duration defaultRetainDuration;
    
    private final Set<Nonce<N>> removedNonces;

    public IncomingRequestManager(Endpoint selfEndpoint, NonceAccessor<N> nonceAccessor) {
        this(selfEndpoint, nonceAccessor, DEFAULT_RETAIN_DURATION);
    }
    
    public IncomingRequestManager(Endpoint selfEndpoint, NonceAccessor<N> nonceAccessor, Duration defaultRetainDuration) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(defaultRetainDuration);

        Validate.isTrue(!defaultRetainDuration.isNegative());

        this.selfEndpoint = selfEndpoint;
        this.nonceAccessor = nonceAccessor;

        this.queue = new PriorityQueue<>(new EventTriggerTimeComparator());
        this.requests = new HashMap<>();
        
        this.defaultRetainDuration = defaultRetainDuration;
        
        this.removedNonces = new HashSet<>();
    }

    public void discardAndTrack(Instant time, Object request, Endpoint srcEndpoint) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        discardAndTrack(time, request, srcEndpoint, defaultRetainDuration);
    }
    
    public void discardAndTrack(Instant time, Object request, Endpoint srcEndpoint, Duration retainDuration)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(srcEndpoint);
        Validate.notNull(retainDuration);

        Validate.isTrue(!retainDuration.isNegative());

        // generate nonce and validate
        Nonce<N> nonce = nonceAccessor.get(request);

        Validate.isTrue(!requests.containsKey(nonce), "Duplicate stored nonce encountered");
        
        // send and queue resends/discards
        requests.put(nonce, new Request(srcEndpoint, request, null));
        queue.add(new DiscardEvent(nonce, time.plus(retainDuration)));
    }
    
    public void sendResponseAndTrack(Instant time, Object request, Object response, Endpoint srcEndpoint) {
        sendResponseAndTrack(time, request, response, srcEndpoint, defaultRetainDuration);
    }

    public void sendResponseAndTrack(Instant time, Object request, Object response, Endpoint srcEndpoint, Duration retainDuration) {
        Validate.notNull(time);
        Validate.notNull(request);
        Validate.notNull(response);
        Validate.notNull(srcEndpoint);
        Validate.notNull(retainDuration);

        Validate.isTrue(!retainDuration.isNegative());

        // generate nonce and validate
        Nonce<N> nonce = nonceAccessor.get(request);
        Validate.isTrue(!requests.containsKey(nonce), "Duplicate stored nonce encountered");
        nonceAccessor.set(response, nonce);
        
        // send and queue resends/discards
        requests.put(nonce, new Request(srcEndpoint, request, response));
        queue.add(new DiscardEvent(nonce, time.plus(retainDuration)));
        
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
