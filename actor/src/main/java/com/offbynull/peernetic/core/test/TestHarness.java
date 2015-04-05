package com.offbynull.peernetic.core.test;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.Context.BatchedOutgoingMessage;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class TestHarness {
    private final MessageDriver messageDriver;
    private final PriorityQueue<Event> events;
    private final String timerPrefix;
    private final Map<String, ActorBundle> actors;
    private Instant lastWhen;
    
    public TestHarness(String timerPrefix) {
        this(Instant.ofEpochMilli(0L), new SimpleMessageDriver(), timerPrefix);
    }
    
    public TestHarness(Instant startTime, String timerPrefix) {
        this(startTime, new SimpleMessageDriver(), timerPrefix);
    }
    
    public TestHarness(Instant startTime, MessageDriver messageDriver, String timerPrefix) {
        Validate.notNull(startTime);
        Validate.notNull(messageDriver);
        Validate.notNull(timerPrefix);

        this.messageDriver = messageDriver;
        this.events = new PriorityQueue<>();
        this.timerPrefix = timerPrefix;
        this.actors = new HashMap<>();
        this.lastWhen = startTime;
    }
    
    public void addActor(String address, Actor actor, Duration timeOffset, Instant when, Object... primingMessages) {
        Validate.notNull(address);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(when);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to add actor event prior to current time");
        Validate.isTrue(!timeOffset.isNegative(), "Negative time offset not allowed");

        events.add(new JoinEvent(address, actor, timeOffset, when));
    }

    public void removeActor(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to remove actor event prior to current time");

        events.add(new LeaveEvent(address, when));
    }

    public void addCustom(Runnable runnable, Instant when) {
        Validate.notNull(runnable);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to add custom event prior to current time");

        events.add(new CustomEvent(runnable, when));
    }

    public boolean hasMore() {
        return !events.isEmpty();
    }
    
    public Instant process() {
        Event event = events.poll();
        Validate.isTrue(event != null, "No events left to process");

        lastWhen = event.getWhen();
        
        if (event instanceof CustomEvent) {
//            CustomEvent customEvent = (CustomEvent) event;
//            
//            Runnable runnable = customEvent.getRunnable();
//            runnable.run();
        } else if (event instanceof MessageEvent) {
//            MessageEvent messageEvent = (MessageEvent) event;
//            
//            Object message = messageEvent.getMessage();
//            String fromId = messageEvent.getSource();
//            String toId = messageEvent.getDestination();
//            
//            ActorBundle srcBundle = actors.get(fromId);
//            ActorBundle dstBundle = actors.get(toId);
//            if (dstBundle != null) {
//                Actor actor = dstBundle.getActor();
//                Endpoint source = srcBundle != null ? srcBundle.getEndpoint() : new InternalEndpoint(fromId); // create fake if not exists
//                Endpoint destination = dstBundle.getEndpoint();
//                Duration timeOffset = dstBundle.getTimeOffset();
//                
//                Instant adjustedLastWhen = lastWhen.plus(timeOffset);
//                
//                try {
//                    actor.onStep(adjustedLastWhen, source, message);
//                } catch (Exception e) {
//                    LOG.error("Actor encountered an error on run", e);
//
//                    try {
//                        actor.onStop(adjustedLastWhen);
//                    } catch (Exception ex) {
//                        LOG.error("Actor encountered an error on stop", ex);
//                    }
//
//                    actors.remove(toId);
//                    actorLookupByEndpoint.remove(destination);
//                }
//            }
        } else if (event instanceof ScheduledMessageEvent) {
//            ScheduledMessageEvent scheduledMessageEvent = (ScheduledMessageEvent) event;
//            
//            Object message = scheduledMessageEvent.getMessage();
//            Endpoint source = scheduledMessageEvent.getSource();
//            Endpoint destination = scheduledMessageEvent.getDestination();
//            
//            ActorBundle dstBundle = actorLookupByEndpoint.get(destination);
//            if (dstBundle != null) {
//                Actor actor = dstBundle.getActor();
//                
//                Duration timeOffset = dstBundle.getTimeOffset();
//                Instant adjustedLastWhen = lastWhen.plus(timeOffset);
//                
//                try {
//                    actor.onStep(adjustedLastWhen, source, message);
//                } catch (Exception e) {
//                    LOG.error("Actor encountered an error on run", e);
//
//                    try {
//                        actor.onStop(adjustedLastWhen);
//                    } catch (Exception ex) {
//                        LOG.error("Actor encountered an error on stop", ex);
//                    }
//
//                    actors.remove(dstBundle.getName());
//                    actorLookupByEndpoint.remove(destination);
//                }
//            }
        } else if (event instanceof JoinEvent) {
            JoinEvent joinEvent = (JoinEvent) event;

            String id = joinEvent.getAddress();
            Actor actor = joinEvent.getActor();
            Duration timeOffset = joinEvent.getTimeOffset();
            UnmodifiableList<Object> primingMessages = joinEvent.primingMessages;
            
            ActorBundle bundle = new ActorBundle(id, actor, timeOffset);

            ActorBundle prevBundle = actors.putIfAbsent(id, bundle);
            Validate.isTrue(prevBundle == null, "Actor identifier already in use");

            Instant adjustedLastWhen = lastWhen.plus(timeOffset);
            processMessages(adjustedLastWhen, id, "management:management", "management:management", primingMessages.toArray());
        } else if (event instanceof LeaveEvent) {
//            LeaveEvent leaveEvent = (LeaveEvent) event;
//
//            A id = leaveEvent.getAddress();
//
//            ActorBundle bundle = actors.get(id);
//            Validate.isTrue(bundle != null, "Actor identifier does not exist");
//            
//            Duration timeOffset = bundle.getTimeOffset();
//            Instant adjustedLastWhen = lastWhen.plus(timeOffset);
//            
//            try {
//                bundle.getActor().onStop(adjustedLastWhen);
//            } catch (Exception ex) {
//                LOG.error("Actor encountered an error on stop", ex);
//            }
//            
//            actors.remove(id);
//            actorLookupByEndpoint.remove(bundle.getEndpoint());
        } else {
            throw new IllegalStateException();
        }
        
        return lastWhen;
    }
    
    private void processMessages(Instant time, String address, String source, String destination, Object ... messages) {
        Validate.notNull(address);
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        ActorBundle actorBundle = actors.get(address);
        Validate.notNull(actorBundle, "Attempting to process messages for non-existant actor {}", address);
        
        Actor actor = actorBundle.getActor();
        Context context = actorBundle.getContext();
        
        try {
            for (Object message : messages) {
                context.setTime(time);
                context.setSource(source);
                context.setDestination(destination);
                context.setSelf(address);
                context.setIncomingMessage(message);
                actor.onStep(context);
                
                List<BatchedOutgoingMessage> batchedOutgoingMessages = context.copyAndClearOutgoingMessages();
                
                for (BatchedOutgoingMessage batchedOutgoingMessage : batchedOutgoingMessages) {
                    String newSourceId = batchedOutgoingMessage.getSourceId();
                    String newDestination = batchedOutgoingMessage.getDestination();
                    Object newMessage = batchedOutgoingMessage.getMessage();
                    String newSource = address + (newSourceId != null ? SEPARATOR + newSourceId : "");
                    
                    events.add(new MessageEvent(newSource, newDestination, newMessage, time));
                }
            }
        } catch (Exception e) {
            actors.remove(address);
        }
    }
}
