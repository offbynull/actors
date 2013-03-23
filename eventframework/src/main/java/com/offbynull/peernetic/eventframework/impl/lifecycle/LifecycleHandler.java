package com.offbynull.peernetic.eventframework.impl.lifecycle;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.handler.OutgoingEventQueue;
import com.offbynull.peernetic.eventframework.helper.StateTracker;
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
