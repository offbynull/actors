package com.offbynull.peernetic.eventframework.handler.generic;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.EventQueuePair;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.handler.OutgoingEventQueue;
import com.offbynull.peernetic.eventframework.helper.SimpleIterateService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class GenericHandler implements Handler {

    private static final Set<Class<? extends OutgoingEvent>> HANDLED_EVENTS;

    static {
        Set<Class<? extends OutgoingEvent>> set = new HashSet<>();
        set.add(ThreadedExecOutgoingEvent.class);
        HANDLED_EVENTS = Collections.unmodifiableSet(set);
    }
    private Service service;

    public GenericHandler() {
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

        private IncomingEventQueue incomingEventQueue;
        private OutgoingEventQueue outgoingEventQueue;
        private ThreadPoolExecutor threadPool;

        @Override
        public void startUp() throws InterruptedException {
            EventQueuePair eventQueuePair =
                    (EventQueuePair) getPassedInObject(0);
            incomingEventQueue = eventQueuePair.getIncomingEventQueue();
            outgoingEventQueue = eventQueuePair.getOutgoingEventQueue();
            threadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }

        @Override
        public boolean iterate() throws InterruptedException {
            List<OutgoingEvent> events = new ArrayList<>();
            outgoingEventQueue.waitForEvents(events);

            for (OutgoingEvent event : events) {
                Class<? extends OutgoingEvent> cls = event.getClass();
                if (cls == ThreadedExecOutgoingEvent.class) {
                    ThreadedExecOutgoingEvent goe = (ThreadedExecOutgoingEvent) event;
                    Callable<?> task = goe.getCallable();
                    long trackedId = goe.getTrackedId();
                    Callable<?> wrappedTask = new DelegCallable<>(
                            incomingEventQueue, task, trackedId);
                    threadPool.submit(wrappedTask);
                } else {
                    // Something went wrong here
                }
            }

            return true;
        }

        @Override
        public void shutDown() throws InterruptedException {
            threadPool.shutdownNow();
        }
    }
    
    private static final class DelegCallable<T> implements Callable<T> {
        private IncomingEventQueue incomingEventQueue;
        private Callable<T> backingCallable;
        private long trackedId;

        public DelegCallable(IncomingEventQueue incomingEventQueue,
                Callable<T> backingCallable, long trackedId) {
            if (incomingEventQueue == null || backingCallable == null) {
                throw new NullPointerException();
            }
            this.incomingEventQueue = incomingEventQueue;
            this.backingCallable = backingCallable;
            this.trackedId = trackedId;
        }

        @Override
        public T call() throws Exception {
            try {
                T ret = backingCallable.call();
                IncomingEvent inEvent = new ThreadedExecResultIncomingEvent(
                        trackedId, ret);
                incomingEventQueue.push(inEvent);
                return ret;
            } catch(Exception e) {
                throw e;
            }
        }
        
    }
}
