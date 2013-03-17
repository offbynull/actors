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
import com.offbynull.peernetic.eventframework.simplifier.IncomingSimplifier;
import com.offbynull.peernetic.eventframework.simplifier.OutgoingSimplifier;
import com.offbynull.peernetic.eventframework.simplifier.SimplifierResult;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Client {

    private Processor rootPattern;
    private Set<Processor<? extends IncomingEvent>> subProcessors;
    private Service service;
    private IncomingEventQueue incomingEventQueue;
    private Map<Class<? extends OutgoingEvent>, OutgoingSimplifier<?>>
            eventOutgoingSimplifierMap;
    private Map<Class<? extends IncomingEvent>, IncomingSimplifier<?>>
            eventIncomingSimplifierMap;
    private Map<Class<? extends OutgoingEvent>, Handler> eventHandlerMap;
    private Map<Handler, OutgoingEventQueue> handlerOutgoingQueueMap;
    private ClientResultListener resultListener;

    public Client(Processor rootPattern, ClientResultListener resultListener,
            Set<Handler> handlers, Set<OutgoingSimplifier> outgoingSimplifiers,
            Set<IncomingSimplifier> incomingSimplifiers) {
        if (rootPattern == null || resultListener == null || handlers == null
                || handlers.contains(null) || outgoingSimplifiers == null
                || outgoingSimplifiers.contains(null)
                || incomingSimplifiers == null
                || incomingSimplifiers.contains(null)) {
            throw new NullPointerException();
        }

        this.rootPattern = rootPattern;
        this.resultListener = resultListener;
        subProcessors = new LinkedHashSet<>();
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
        
        
        // process simplifiers
        eventOutgoingSimplifierMap = new HashMap<>();
        
        for (OutgoingSimplifier simplifier : outgoingSimplifiers) {
            Set<Class<? extends OutgoingEvent>> acceptedTypes =
                    simplifier.viewHandledEvents();
            
            for (Class<? extends OutgoingEvent> type : acceptedTypes) {
                if (eventHandlerMap.containsKey(type)
                        || eventOutgoingSimplifierMap.containsKey(type)) {
                    throw new IllegalArgumentException();
                }
                eventOutgoingSimplifierMap.put(type, simplifier);
            }
        }
        
        
        // process incoming simplifiers
        eventIncomingSimplifierMap = new HashMap<>();
        
        for (IncomingSimplifier simplifier : incomingSimplifiers) {
            Set<Class<? extends IncomingEvent>> acceptedTypes =
                    simplifier.viewHandledEvents();
            
            for (Class<? extends IncomingEvent> type : acceptedTypes) {
                if (eventIncomingSimplifierMap.containsKey(type)) {
                    throw new IllegalArgumentException();
                }
                eventIncomingSimplifierMap.put(type, simplifier);
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
                // get timestamp to pass in to processors
                long timestamp = System.currentTimeMillis();
                
                // create event pools
                LinkedList<OutgoingEvent> totalOutEvents = new LinkedList<>();
                LinkedList<IncomingEvent> totalInEvents = new LinkedList<>();

                // grab events from handlers and send to simplifiers
                // (potentially pruned by simplifiers)
                LinkedList<IncomingEvent> initialInEvents = new LinkedList<>();
                incomingEventQueue.waitForEvents(initialInEvents);
                sendToIncomingSimplifiers(initialInEvents);
                
                // add to in events pool
                totalInEvents.addAll(initialInEvents);

                // process
                while (!totalInEvents.isEmpty()) {
                    // get event
                    IncomingEvent inEvent = totalInEvents.pollFirst();
                    
                    // pass to root processor and see if it wants to finish
                    ProcessResult<?> result = rootPattern.process(timestamp,
                            inEvent);
                    if (checkRootProcessorResult(timestamp, result)) {
                        return false;
                    }

                    // grab out events from root proc & send to out simplifiers
                    List<OutgoingEvent> newOutEvents = new LinkedList<>(
                            result.viewOutgoingEvents());
                    sendToOutgoingSimplifiers(newOutEvents);
                    
                    // put remaining out events in newOutEvents in to pull
                    totalOutEvents.addAll(newOutEvents);
                    
                    
                    // process subprocessors
                    while (true) {
                        // create pools
                        List<OutgoingEvent> newSubOutEvents = new LinkedList<>();
                        List<IncomingEvent> newSubInEvents = new LinkedList<>();
                        
                        // call subprocessors -- pools filled here
                        callSubProcessors(timestamp, inEvent, newSubOutEvents,
                                newSubInEvents);

                        // send inevent pool to incoming simplifiers
                        sendToIncomingSimplifiers(newSubInEvents);
                        
                        // send outevent pool to outgoing simplifiers
                        sendToOutgoingSimplifiers(newSubOutEvents);
                        
                        // add new events to totals
                        totalOutEvents.addAll(newSubOutEvents);
                        totalInEvents.addAll(newSubInEvents);
                        
                        // if no new outevents from subprocessor, break out of
                        // loop
                        if (newSubOutEvents.isEmpty()) {
                            break;
                        }
                    }
                }

                // send to handlers
                sendToHandlers(totalOutEvents);
                
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
        
        private void callSubProcessors(long timestamp, IncomingEvent inEvent,
                List<OutgoingEvent> newOutEvents,
                List<IncomingEvent> newInEvents) {
            
            Iterator<Processor<? extends IncomingEvent>> it =
                    subProcessors.iterator();
            
            while (it.hasNext()) {
                Processor<? extends IncomingEvent> proc = it.next();
                
                try {
                    ProcessResult<? extends IncomingEvent> result =
                            proc.process(timestamp, inEvent);
                    newOutEvents.addAll(result.viewOutgoingEvents());
                    
                    if (result instanceof FinishedProcessResult) {
                        FinishedProcessResult<? extends IncomingEvent> finRes =
                                (FinishedProcessResult<? extends IncomingEvent>)
                                result;
                        
                        newInEvents.add(finRes.getResult());
                        it.remove();
                    }
                } catch (Exception e) {
                    it.remove();
                }
            }
        }
        
        private void sendToOutgoingSimplifiers(List<OutgoingEvent> outEvents) {
            
            LinkedList<Processor<? extends IncomingEvent>> newProcs =
                        new LinkedList<>();

            Iterator<OutgoingEvent> it = outEvents.iterator();
            while (it.hasNext()) {
                OutgoingEvent outEvent = it.next();
                
                // handle if simplifier
                OutgoingSimplifier<?> simplifier = eventOutgoingSimplifierMap.get(
                        outEvent.getClass());

                if (simplifier != null) {
                    SimplifierResult<? extends IncomingEvent> res = null;
                    
                    try {
                        res = simplifier.simplify(outEvent); 
                    } catch (Exception e) {
                        // do nothing
                    }
                    
                    if (res != null) {
                        newProcs.add(res.getNewProcessor());
                        if (res.isConsumeEvent()) {
                            it.remove();
                        }
                    }
                }
            }
            
            subProcessors.addAll(newProcs);
        }
        
        private void sendToIncomingSimplifiers(List<IncomingEvent> inEvents) {
            
            LinkedList<Processor<? extends IncomingEvent>> newProcs =
                        new LinkedList<>();

            Iterator<IncomingEvent> it = inEvents.iterator();
            while (it.hasNext()) {
                IncomingEvent inEvent = it.next();
                
                // handle if simplifier
                IncomingSimplifier<?> simplifier =
                        eventIncomingSimplifierMap.get(inEvent.getClass());

                if (simplifier != null) {
                    SimplifierResult<? extends IncomingEvent> res = null;
                    
                    try {
                        res = simplifier.simplify(inEvent); 
                    } catch (Exception e) {
                        // do nothing
                    }
                    
                    if (res != null) {
                        newProcs.add(res.getNewProcessor());
                        if (res.isConsumeEvent()) {
                            it.remove();
                        }
                    }
                }
            }
            
            subProcessors.addAll(newProcs);
        }
        
        private void sendToHandlers(List<OutgoingEvent> outEvents) {
            for (OutgoingEvent outEvent : outEvents) {
                Handler handler = eventHandlerMap.get(
                        outEvent.getClass());

                if (handler != null) {
                    OutgoingEventQueue outEventQueue =
                            handlerOutgoingQueueMap.get(handler);
                    outEventQueue.push(outEvent);
                } else {
                    throw new IllegalStateException();
                }
            }
        }

        private boolean checkRootProcessorResult(long timestamp,
                ProcessResult result) {
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
