package com.offbynull.eventframework.handler.lifecycle;

import com.offbynull.eventframework.handler.OutgoingEvent;
import com.offbynull.eventframework.handler.Handler;
import com.offbynull.eventframework.handler.IncomingEventQueue;
import com.offbynull.eventframework.handler.OutgoingEventQueue;
import com.offbynull.eventframework.helper.StateTracker;
import java.util.Collections;
import java.util.Set;

public final class LifecycleHandler implements Handler {
    private static final Set<Class<? extends OutgoingEvent>> HANDLED_EVENTS;
    static {
        HANDLED_EVENTS = Collections.emptySet();
    }
    
    private StateTracker stateTracker;
    private OutgoingEventQueue outgoingEventQueue;

    public LifecycleHandler() {
        stateTracker = new StateTracker();
        outgoingEventQueue = new OutgoingEventQueue(HANDLED_EVENTS);
    }

    @Override
    public Set<Class<? extends OutgoingEvent>> viewHandledEvents() {
        return HANDLED_EVENTS;
    }

    @Override
    public OutgoingEventQueue start(IncomingEventQueue ieq) {
        stateTracker.start();
        ieq.push(new InitializeIncomingEvent());
        return outgoingEventQueue;
    }

    @Override
    public void stop() {
        stateTracker.stop();
    }
}
