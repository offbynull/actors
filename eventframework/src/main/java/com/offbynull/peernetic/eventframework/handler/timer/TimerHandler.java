package com.offbynull.peernetic.eventframework.handler.timer;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.EventQueuePair;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.handler.OutgoingEventQueue;
import com.offbynull.peernetic.eventframework.helper.SimpleIterateService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public final class TimerHandler implements Handler {
    private static final Set<Class<? extends OutgoingEvent>> HANDLED_EVENTS;
    static {
        Set<Class<? extends OutgoingEvent>> set = new HashSet<>();
        set.add(NewTimerOutgoingEvent.class);
        HANDLED_EVENTS = Collections.unmodifiableSet(set);
    }

    private Service service;

    public TimerHandler() {
        service = new Service();
    }

    @Override
    public Set<Class<? extends OutgoingEvent>> viewHandledEvents() {
        return HANDLED_EVENTS;
    }

    @Override
    public OutgoingEventQueue start(IncomingEventQueue incomingEventQueue) {
        if (incomingEventQueue == null) {
            throw new NullPointerException();
        }

        OutgoingEventQueue outgoingEventQueue =
                new OutgoingEventQueue(HANDLED_EVENTS);

        EventQueuePair eventQueuePair =
                new EventQueuePair(incomingEventQueue, outgoingEventQueue);
        service.safeStartAndWait(eventQueuePair);
        
        return outgoingEventQueue;
    }

    @Override
    public void stop() {
        service.safeStopAndWait();
    }
    
    private static final class Service extends SimpleIterateService {
        private Timer timer;
        private IncomingEventQueue incomingEventQueue;
        private OutgoingEventQueue outgoingEventQueue;
        
        @Override
        public void startUp() throws InterruptedException {
            EventQueuePair eventQueuePair =
                    (EventQueuePair) getPassedInObject(0);
            incomingEventQueue = eventQueuePair.getIncomingEventQueue();
            outgoingEventQueue = eventQueuePair.getOutgoingEventQueue();
            timer = new Timer(true);
        }

        @Override
        public boolean iterate() throws InterruptedException {
            List<OutgoingEvent> events = new ArrayList<>();
            outgoingEventQueue.waitForEvents(events);

            for (OutgoingEvent event : events) {
                Class<? extends OutgoingEvent> cls = event.getClass();
                if (cls == NewTimerOutgoingEvent.class) {
                    NewTimerOutgoingEvent ntoe = (NewTimerOutgoingEvent) event;
                    long sequenceId = ntoe.getTrackedId();
                    long delay = ntoe.getDuration();
                    Object data = ntoe.getData();
                    TimerTask timerTask = new CustomTimerTask(sequenceId, data,
                            incomingEventQueue);
                    timer.schedule(timerTask, delay);
                } else {
                    // Something went wrong here
                }
            }
            
            return true;
        }

        @Override
        public void shutDown() throws InterruptedException {
            timer.cancel();
        }
    }
    
    private static final class CustomTimerTask extends TimerTask {
        private long sequenceId;
        private Object data;
        private IncomingEventQueue incomingEventQueue;

        public CustomTimerTask(long sequenceId, Object data,
                IncomingEventQueue incomingEventQueue) {
            this.sequenceId = sequenceId;
            this.data = data;
            this.incomingEventQueue = incomingEventQueue;
        }

        @Override
        public void run() {
            incomingEventQueue.push(
                    new TimerHitIncomingEvent(sequenceId, data));
        }
    }
}
