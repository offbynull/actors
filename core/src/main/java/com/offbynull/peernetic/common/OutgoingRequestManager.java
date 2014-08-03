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

public final class OutgoingRequestManager<T> {
    private Endpoint selfEndpoint;
    private NonceManager<T> nonceManager;
    private NonceGenerator<T> nonceGenerator;

    public OutgoingRequestManager(Endpoint selfEndpoint, NonceManager<T> nonceManager, NonceGenerator<T> nonceGenerator) {
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceManager);
        Validate.notNull(nonceGenerator);
        
        this.selfEndpoint = selfEndpoint;
        this.nonceManager = nonceManager;
        this.nonceGenerator = nonceGenerator;
    }
    
    public void send(Object request, Endpoint destination, Duration giveUpDuration, Duration resendDuration) {
        FILLMEIN;
        //validate before adding to queue and sending
    }
    
    public HandleResult handle() {
        FILLMEIN;
        return null;
    }
    
    public static final class HandleResult {
        private Duration waitDuration;
        private UnmodifiableList<Object> droppedRequests;
        private UnmodifiableList<Object> answeredRequests;

        public HandleResult(Duration waitDuration, UnmodifiableList<Object> droppedRequests, UnmodifiableList<Object> answeredRequests) {
            this.waitDuration = waitDuration;
            this.droppedRequests = droppedRequests;
            this.answeredRequests = answeredRequests;
        }

        public Duration getWaitDuration() {
            return waitDuration;
        }

        public UnmodifiableList<Object> getDroppedRequests() {
            return droppedRequests;
        }

        public UnmodifiableList<Object> getAnsweredRequests() {
            return answeredRequests;
        }
    }
    
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface NonceField {
    
    }
}
