package com.offbynull.peernetic.eventframework;

import com.google.common.util.concurrent.Service.State;
import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import com.offbynull.peernetic.eventframework.handler.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.handler.OutgoingEventQueue;
import com.offbynull.peernetic.eventframework.helper.SimpleIterateService;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Client {

    private Processor rootPattern;
    private Service service;
    private IncomingEventQueue incomingEventQueue;
    private Map<Class<? extends OutgoingEvent>, Handler> eventHandlerMap;
    private Map<Handler, OutgoingEventQueue> handlerOutgoingQueueMap;
    private ClientResultListener resultListener;

    public Client(Processor rootPattern, ClientResultListener resultListener,
            Set<Handler> handlers) {
        if (rootPattern == null || resultListener == null || handlers == null
                || handlers.contains(null)) {
            throw new NullPointerException();
        }

        this.rootPattern = rootPattern;
        this.resultListener = resultListener;
        service = new Service();
        incomingEventQueue = new IncomingEventQueue();
        eventHandlerMap = new HashMap<>();
        handlerOutgoingQueueMap = new HashMap<>();
        
        for (Handler handler : handlers) {
            OutgoingEventQueue outgoingEventQueue =
                    handler.start(incomingEventQueue);
            handlerOutgoingQueueMap.put(handler, outgoingEventQueue);

            Set<Class<? extends OutgoingEvent>> acceptedTypes =
                    handler.viewHandledEvents();
            for (Class<? extends OutgoingEvent> type : acceptedTypes) {
                eventHandlerMap.put(type, handler);
            }
        }
    }

    public void start() {
        service.safeStartAndWait();
    }

    public void stop() {
        service.safeStopAndWait();
    }

    private final class Service extends SimpleIterateService {

        @Override
        public boolean iterate() throws InterruptedException {
            try {
                List<IncomingEvent> incomingEvents = new ArrayList<>();
                incomingEventQueue.waitForEvents(incomingEvents);

                long timestamp = System.currentTimeMillis();

                for (IncomingEvent incomingEvent : incomingEvents) {
                    ProcessResult result = rootPattern.process(
                            timestamp, incomingEvent);

                    List<OutgoingEvent> outgoingEvents =
                            result.viewOutgoingEvents();

                    for (OutgoingEvent outgoingEvent : outgoingEvents) {
                        Handler handler = eventHandlerMap.get(
                                outgoingEvent.getClass());
                        OutgoingEventQueue outgoingEventQueue
                                = handlerOutgoingQueueMap.get(handler);
                        outgoingEventQueue.push(outgoingEvent);
                    }

                    if (result instanceof OngoingProcessResult) {
                        return true;
                    } else if (result instanceof FinishedProcessResult) {
                        FinishedProcessResult finishedResult =
                                (FinishedProcessResult) result;
                        if (resultListener != null) {
                            resultListener.processorFinished(timestamp,
                                    finishedResult.getResult());
                        }
                        return false;
                    } else {
                        throw new IllegalStateException();
                    }
                }

                return true;
            } catch (InterruptedException ie) {
                throw ie;
            } catch (Exception e) {
                if (resultListener != null) {
                    resultListener.processorException(e);
                }
                
                return false;
            }
        }

        @Override
        public void shutDown() throws InterruptedException {
            for (Handler handler : handlerOutgoingQueueMap.keySet()) {
                try {
                    handler.stop();
                } catch (RuntimeException re) {
                    // do nothing
                }
            }
        }
    }
}
