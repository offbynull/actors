package com.offbynull.eventframework.handler;

public final class EventQueuePair {
    private IncomingEventQueue incomingEventQueue;
    private OutgoingEventQueue outgoingEventQueue;

    public EventQueuePair(IncomingEventQueue incomingEventQueue,
            OutgoingEventQueue outgoingEventQueue) {
        if (incomingEventQueue == null || outgoingEventQueue == null) {
            throw new NullPointerException();
        }
        this.incomingEventQueue = incomingEventQueue;
        this.outgoingEventQueue = outgoingEventQueue;
    }

    public IncomingEventQueue getIncomingEventQueue() {
        return incomingEventQueue;
    }

    public OutgoingEventQueue getOutgoingEventQueue() {
        return outgoingEventQueue;
    }
    
}
