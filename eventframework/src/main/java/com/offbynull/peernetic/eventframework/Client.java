package com.offbynull.peernetic.eventframework;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.handler.Handler;
import com.offbynull.peernetic.eventframework.handler.IncomingEventQueue;
import com.offbynull.peernetic.eventframework.handler.OutgoingEventQueue;
import com.offbynull.peernetic.eventframework.helper.SimpleIterateService;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.interceptor.OutgoingInterceptor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Client {

    private Processor rootPattern;
    private Service service;
    private IncomingEventQueue incomingEventQueue;
    private Map<Class<? extends OutgoingEvent>, OutgoingInterceptor<?>>
            eventSimplifierMap;
    private Map<Class<? extends OutgoingEvent>, Handler> eventHandlerMap;
    private Map<Handler, OutgoingEventQueue> handlerOutgoingQueueMap;
    private ClientResultListener resultListener;

    public Client(Processor rootPattern, ClientResultListener resultListener,
            Set<Handler> handlers, Set<OutgoingInterceptor> simplifiers) {
        if (rootPattern == null || resultListener == null || handlers == null
                || handlers.contains(null) || simplifiers == null
                || simplifiers.contains(null)) {
            throw new NullPointerException();
        }

        this.rootPattern = rootPattern;
        this.resultListener = resultListener;
        service = new Service();
        incomingEventQueue = new IncomingEventQueue();
        eventHandlerMap = new HashMap<>();
        handlerOutgoingQueueMap = new HashMap<>();
        
        // process handlers
        for (Handler handler : handlers) {
            OutgoingEventQueue outgoingEventQueue =
                    handler.start(incomingEventQueue);
            handlerOutgoingQueueMap.put(handler, outgoingEventQueue);

            Set<Class<? extends OutgoingEvent>> acceptedTypes =
                    handler.viewHandledEvents();
            for (Class<? extends OutgoingEvent> type : acceptedTypes) {
                if (eventHandlerMap.containsKey(type)) {
                    throw new IllegalArgumentException();
                }
                eventHandlerMap.put(type, handler);
            }
        }
        
        
        eventSimplifierMap = new HashMap<>();
        
        // process simplifiers
        for (OutgoingInterceptor simplifier : simplifiers) {
            Set<Class<? extends OutgoingEvent>> acceptedTypes =
                    simplifier.viewHandledEvents();
            
            for (Class<? extends OutgoingEvent> type : acceptedTypes) {
                if (eventHandlerMap.containsKey(type)
                        || eventSimplifierMap.containsKey(type)) {
                    throw new IllegalArgumentException();
                }
                eventSimplifierMap.put(type, simplifier);
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
                LinkedList<IncomingEvent> inEvents = new LinkedList<>();
                incomingEventQueue.waitForEvents(inEvents);

                long timestamp = System.currentTimeMillis();

                while (!inEvents.isEmpty()) {
                    IncomingEvent incomingEvent = inEvents.pollFirst();
                    
                    ProcessResult result = rootPattern.process(
                            timestamp, incomingEvent);

                    List<OutgoingEvent> outEvents = result.viewOutgoingEvents();

                    for (OutgoingEvent outgoingEvent : outEvents) {
                        List<IncomingEvent> additionalInEvents =
                                processOutgoingEvents(timestamp, outgoingEvent);
                        inEvents.addAll(additionalInEvents);
                    }

                    if (checkIfFinished(timestamp, result)) {
                        return false;
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

        private List<IncomingEvent> processOutgoingEvents(long timestamp,
                OutgoingEvent outgoingEvent) throws Exception {
            
            LinkedList<IncomingEvent> inEvents = new LinkedList<>();
            
            LinkedList<OutgoingEvent> outEvents = new LinkedList<>();
            outEvents.add(outgoingEvent);
            
            while (!outEvents.isEmpty()) {
                OutgoingEvent outEvent = outEvents.pollFirst();
                
                // handle if simplifier
                OutgoingInterceptor<?> simplifier = eventSimplifierMap.get(
                        outEvent.getClass());

                if (simplifier != null) {
                    List<IncomingEvent> retInEvents = new LinkedList<>();
                    List<OutgoingEvent> retOutEvents = new LinkedList<>();
                    
                    simplifier.intercept(timestamp, outEvent, retInEvents,
                            retOutEvents);
                    
                    outEvents.addAll(retOutEvents);
                    inEvents.addAll(retInEvents);
                    
                    continue;
                }


                // handle if handler
                Handler handler = eventHandlerMap.get(
                        outEvent.getClass());

                if (handler != null) {
                    OutgoingEventQueue outgoingEventQueue
                            = handlerOutgoingQueueMap.get(handler);
                    outgoingEventQueue.push(outEvent);
                    continue;
                }


                // can't handle
                throw new IllegalStateException();
            }
            
            return inEvents;
        }

        private boolean checkIfFinished(long timestamp, ProcessResult result) {
            if (result instanceof OngoingProcessResult) {
                // do nothing
            } else if (result instanceof FinishedProcessResult) {
                FinishedProcessResult finishedResult =
                        (FinishedProcessResult) result;
                if (resultListener != null) {
                    resultListener.processorFinished(timestamp,
                            finishedResult.getResult());
                }
                return true;
            } else {
                throw new IllegalStateException();
            }
            return false;
        }
    }
}
