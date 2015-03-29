package com.offbynull.peernetic.core.actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class Context {
    private String selfPrefix;
    private String selfId;
    private Instant time;
    private String source;
    private Object incomingMessage;
    private List<BatchedOutgoingMessage> outgoingMessages = new LinkedList<>();

    public String getSelfPrefix() {
        return selfPrefix;
    }

    void setSelfPrefix(String selfPrefix) {
        this.selfPrefix = selfPrefix;
    }

    public String getSelfId() {
        return selfId;
    }

    void setSelfId(String selfId) {
        this.selfId = selfId;
    }

    public Instant getTime() {
        return time;
    }

    void setTime(Instant time) {
        Validate.notNull(time);
        this.time = time;
    }

    public String getSource() {
        return source;
    }

    void setSource(String source) {
        Validate.notNull(source);
        this.source = source;
    }

    public <T> T getIncomingMessage() {
        return (T) incomingMessage;
    }

    void setIncomingMessage(Object incomingMessage) {
        Validate.notNull(incomingMessage);
        this.incomingMessage = incomingMessage;
    }

    public void addOutgoingMessage(String destination, Object message) {
        Validate.notNull(destination);
        Validate.notNull(message);
        outgoingMessages.add(new BatchedOutgoingMessage(destination, message));
    }
    
    List<BatchedOutgoingMessage> copyAndClearOutgoingMessages() {
        List<BatchedOutgoingMessage> ret = new ArrayList<>(outgoingMessages);
        outgoingMessages.clear();
        
        return ret;
    }
    
    static final class BatchedOutgoingMessage {
        private final String destination;
        private final Object message;

        public BatchedOutgoingMessage(String destination, Object message) {
            Validate.notNull(destination);
            Validate.notNull(message);
            Validate.notEmpty(destination);
            
            this.destination = destination;
            this.message = message;
        }

        public String getDestination() {
            return destination;
        }

        public Object getMessage() {
            return message;
        }        
    }
    
}
