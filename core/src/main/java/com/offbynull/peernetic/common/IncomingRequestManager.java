package com.offbynull.peernetic.common;

import com.offbynull.peernetic.actor.Endpoint;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class IncomingRequestManager<T> {
    private Endpoint selfEndpoint;
    private NonceManager<T> nonceManager;
    private NonceGenerator<T> nonceGenerator;

    public IncomingRequestManager(Endpoint selfEndpoint, NonceManager<T> nonceManager, NonceGenerator<T> nonceGenerator) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceManager);
        Validate.notNull(nonceGenerator);
        
        this.selfEndpoint = selfEndpoint;
        this.nonceManager = nonceManager;
        this.nonceGenerator = nonceGenerator;
    }
    
    // respond with amount of time to wait before calling handle
    public RequestResult receive(Object request, Endpoint source, Duration discardDuration) {
        FILLMEIN;
    }

    // false if request cannot be found for response
    public boolean respond(Object request, Object response, Endpoint source) {
        FILLMEIN;
    }

    public HandleResult handle() {
        FILLMEIN;
        return null;
    }
    
    public static final class RequestResult {
        private Duration waitDuration;
        private Object request;

        public RequestResult(Duration waitDuration, Object request) {
            this.waitDuration = waitDuration;
            this.request = request;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public Object getRequest() {
            return request;
        }
    }

    public static final class HandleResult {
        private Duration waitDuration;
        private UnmodifiableList<Object> discardedAnsweredRequests;
        private UnmodifiableList<Object> discardedUnansweredRequests;

        public HandleResult(Duration waitDuration, UnmodifiableList<Object> discardedAnsweredRequests, UnmodifiableList<Object> discardedUnansweredRequests) {
            this.waitDuration = waitDuration;
            this.discardedAnsweredRequests = discardedAnsweredRequests;
            this.discardedUnansweredRequests = discardedUnansweredRequests;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public UnmodifiableList<Object> getDiscardedAnsweredRequests() {
            return discardedAnsweredRequests;
        }

        public UnmodifiableList<Object> getDiscardedUnansweredRequests() {
            return discardedUnansweredRequests;
        }

    }
    
}
