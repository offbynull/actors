package com.offbynull.peernetic.debug.testharness;

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.actor.NullEndpoint;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestHarness<A> {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestHarness.class);

    private MessageDriver<A> messageDriver;
    private PriorityQueue<Event> events;
    private Map<A, ActorBundle> actorLookupById;
    private Map<Endpoint, ActorBundle> actorLookupByEndpoint;
    private EndpointScheduler endpointScheduler;
    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private Instant lastWhen;

    public TestHarness() {
        this(Instant.ofEpochMilli(0L), new BasicMessageDriver<>());
    }
    
    public TestHarness(Instant startTime) {
        this(startTime, new BasicMessageDriver<>());
    }
    
    public TestHarness(Instant startTime, MessageDriver messageDriver) {
        Validate.notNull(startTime);
        Validate.notNull(messageDriver);

        this.messageDriver = messageDriver;
        this.events = new PriorityQueue<>();
        this.actorLookupById = new HashMap<>();
        this.actorLookupByEndpoint = new HashMap<>();
        this.endpointScheduler = new InternalEndpointScheduler();
        this.endpointIdentifier = new InternalEndpointIdentifier();
        this.endpointDirectory = new InternalEndpointDirectory();
        this.lastWhen = startTime;
    }

    public void addActor(A id, Actor actor, Duration timeOffset, Instant when) {
        Validate.notNull(id);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to add actor event prior to current time");
        Validate.isTrue(!timeOffset.isNegative(), "Negative time offset not allowed");

        events.add(new JoinEvent(id, actor, timeOffset, when));
    }

    public void removeActor(A id, Instant when) {
        Validate.notNull(id);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to remove actor event prior to current time");

        events.add(new LeaveEvent(id, when));
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
        
        if (event instanceof TestHarness.CustomEvent) {
            CustomEvent customEvent = (CustomEvent) event;
            
            Runnable runnable = customEvent.getRunnable();
            runnable.run();
        } else if (event instanceof TestHarness.MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            
            Object message = messageEvent.getMessage();
            A fromId = messageEvent.getFromId();
            A toId = messageEvent.getToId();
            
            ActorBundle srcBundle = actorLookupById.get(fromId);
            ActorBundle dstBundle = actorLookupById.get(toId);
            if (dstBundle != null) {
                Actor actor = dstBundle.getActor();
                Endpoint source = srcBundle != null ? srcBundle.getEndpoint() : new InternalEndpoint(fromId); // create fake if not exists
                Endpoint destination = dstBundle.getEndpoint();
                Duration timeOffset = dstBundle.getTimeOffset();
                
                Instant adjustedLastWhen = lastWhen.plus(timeOffset);
                
                try {
                    actor.onStep(adjustedLastWhen, source, message);
                } catch (Exception e) {
                    LOG.error("Actor encountered an error on run", e);

                    try {
                        actor.onStop(adjustedLastWhen);
                    } catch (Exception ex) {
                        LOG.error("Actor encountered an error on stop", ex);
                    }

                    actorLookupById.remove(toId);
                    actorLookupByEndpoint.remove(destination);
                }
            }
        } else if (event instanceof TestHarness.ScheduledMessageEvent) {
            ScheduledMessageEvent scheduledMessageEvent = (ScheduledMessageEvent) event;
            
            Object message = scheduledMessageEvent.getMessage();
            Endpoint source = scheduledMessageEvent.getSource();
            Endpoint destination = scheduledMessageEvent.getDestination();
            
            ActorBundle dstBundle = actorLookupByEndpoint.get(destination);
            if (dstBundle != null) {
                Actor actor = dstBundle.getActor();
                
                Duration timeOffset = dstBundle.getTimeOffset();
                Instant adjustedLastWhen = lastWhen.plus(timeOffset);
                
                try {
                    actor.onStep(adjustedLastWhen, source, message);
                } catch (Exception e) {
                    LOG.error("Actor encountered an error on run", e);

                    try {
                        actor.onStop(adjustedLastWhen);
                    } catch (Exception ex) {
                        LOG.error("Actor encountered an error on stop", ex);
                    }

                    actorLookupById.remove(dstBundle.getName());
                    actorLookupByEndpoint.remove(destination);
                }
            }
        } else if (event instanceof TestHarness.JoinEvent) {
            JoinEvent joinEvent = (JoinEvent) event;

            A id = joinEvent.getId();
            Actor actor = joinEvent.getActor();
            InternalEndpoint endpoint = new InternalEndpoint(id);
            Duration timeOffset = joinEvent.getTimeOffset();
            
            ActorBundle bundle = new ActorBundle(id, actor, endpoint, timeOffset);

            ActorBundle prevBundle = actorLookupById.putIfAbsent(id, bundle);
            Validate.isTrue(prevBundle == null, "Actor identifier already in use");
            actorLookupByEndpoint.put(endpoint, bundle);

            Instant adjustedLastWhen = lastWhen.plus(timeOffset);
            
            try {
                actor.onStart(adjustedLastWhen);
            } catch (Exception e) {
                LOG.error("Actor encountered an error on start", e);
                
                try {
                    actor.onStop(adjustedLastWhen);
                } catch (Exception ex) {
                    LOG.error("Actor encountered an error on stop", ex);
                }
                
                actorLookupById.remove(id);
                actorLookupByEndpoint.remove(endpoint);
            }
        } else if (event instanceof TestHarness.LeaveEvent) {
            LeaveEvent leaveEvent = (LeaveEvent) event;

            A id = leaveEvent.getId();

            ActorBundle bundle = actorLookupById.get(id);
            Validate.isTrue(bundle != null, "Actor identifier does not exist");
            
            Duration timeOffset = bundle.getTimeOffset();
            Instant adjustedLastWhen = lastWhen.plus(timeOffset);
            
            try {
                bundle.getActor().onStop(adjustedLastWhen);
            } catch (Exception ex) {
                LOG.error("Actor encountered an error on stop", ex);
            }
            
            actorLookupById.remove(id);
            actorLookupByEndpoint.remove(bundle.getEndpoint());
        } else {
            throw new IllegalStateException();
        }
        
        return lastWhen;
    }

    public Endpoint getEndpoint(A name) {
        Validate.notNull(name);
        
        ActorBundle bundle = actorLookupById.get(name);
        Validate.isTrue(bundle != null, "No actor found");
        
        return bundle.getEndpoint();
    }
    
    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public EndpointDirectory<A> getEndpointDirectory() {
        return endpointDirectory;
    }

    public EndpointIdentifier<A> getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public void scheduleFromNull(Duration duration, A id, Object message) {
        endpointScheduler.scheduleMessage(duration, NullEndpoint.INSTANCE, new InternalEndpoint(id), message);
    }
    
    private final class InternalEndpoint implements Endpoint {
        private final A id;

        public InternalEndpoint(A id) {
            Validate.notNull(id);
            this.id = id;
        }
        
        @Override
        public void send(Endpoint source, Object message) {
            ActorBundle sourceActorBundle = actorLookupByEndpoint.get(source);
            
            List<MessageEnvelope<A>> messageEnvelopes = messageDriver.onMessageSend(sourceActorBundle.getName(), id, message);
            messageEnvelopes.forEach(x -> {
                Validate.isTrue(!x.getDuration().isNegative(), "Negative duration not allowed");
                Instant when = lastWhen.plus(x.getDuration());
                events.add(new MessageEvent(x.getSender(), x.getReceiver(), x.getMessage(), when));
            });
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final InternalEndpoint other = (InternalEndpoint) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return true;
        }
        
    }
    
    private final class ActorBundle {
        private final A id;
        private final Actor actor;
        private final Endpoint endpoint;
        private final Duration timeOffset;

        public ActorBundle(A id, Actor actor, Endpoint endpoint, Duration timeOffset) {
            Validate.notNull(id);
            Validate.notNull(actor);
            Validate.notNull(endpoint);
            Validate.notNull(timeOffset);
            Validate.isTrue(!timeOffset.isNegative());
            
            this.id = id;
            this.actor = actor;
            this.endpoint = endpoint;
            this.timeOffset = timeOffset;
        }

        public A getName() {
            return id;
        }

        public Actor getActor() {
            return actor;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public Duration getTimeOffset() {
            return timeOffset;
        }
        
    }
    
    private abstract class Event implements Comparable<Event> {

        private final Instant when;

        public Event(Instant when) {
            this.when = when;
        }

        public Instant getWhen() {
            return when;
        }

        @Override
        public int compareTo(Event o) {
            return when.compareTo(o.when); // smallest instant to largest instant
        }
    }

    private final class MessageEvent extends Event {

        private final A fromId;
        private final A toId;
        private final Object message;

        public MessageEvent(A fromId, A toId, Object message, Instant when) {
            super(when);
            this.fromId = fromId;
            this.toId = toId;
            this.message = message;
        }

        public A getFromId() {
            return fromId;
        }

        public A getToId() {
            return toId;
        }

        public Object getMessage() {
            return message;
        }

    }

    private final class CustomEvent extends Event {

        private final Runnable runnable;

        public CustomEvent(Runnable runnable, Instant when) {
            super(when);
            this.runnable = runnable;
        }

        public Runnable getRunnable() {
            return runnable;
        }
    }

    private final class ScheduledMessageEvent extends Event {

        private final Endpoint source;
        private final Endpoint destination;
        private final Object message;

        public ScheduledMessageEvent(Endpoint source, Endpoint destination, Object message, Instant when) {
            super(when);
            this.source = source;
            this.destination = destination;
            this.message = message;
        }

        public Endpoint getSource() {
            return source;
        }

        public Endpoint getDestination() {
            return destination;
        }

        public Object getMessage() {
            return message;
        }

    }
    
    private final class JoinEvent extends Event {

        private final A id;
        private final Actor actor;
        private final Duration timeOffset;

        public JoinEvent(A id, Actor actor, Duration timeOffset, Instant when) {
            super(when);
            this.id = id;
            this.actor = actor;
            this.timeOffset = timeOffset;
        }

        public A getId() {
            return id;
        }

        public Actor getActor() {
            return actor;
        }

        public Duration getTimeOffset() {
            return timeOffset;
        }
        
    }

    private final class LeaveEvent extends Event {

        private final A id;

        public LeaveEvent(A id, Instant when) {
            super(when);
            this.id = id;
        }

        public A getId() {
            return id;
        }

    }

    public interface MessageDriver<A> {

        List<MessageEnvelope<A>> onMessageSend(A sender, A receiver, Object message);
    }
    
    public static final class BasicMessageDriver<A> implements MessageDriver<A> {

        @Override
        public List<MessageEnvelope<A>> onMessageSend(A sender, A receiver, Object message) {
            return Collections.singletonList(new MessageEnvelope<>(sender, receiver, message, Duration.ZERO));
        }
        
    }

    public static final class MessageEnvelope<A> {

        private final Object message;
        private final A sender;
        private final A receiver;
        private final Duration duration;

        public MessageEnvelope(A sender, A receiver, Object message, Duration duration) {
            Validate.notNull(sender);
            Validate.notNull(receiver);
            Validate.notNull(message);
            Validate.notNull(duration);

            this.sender = sender;
            this.receiver = receiver;
            this.message = message;
            this.duration = duration;
        }

        public Object getMessage() {
            return message;
        }

        public A getSender() {
            return sender;
        }

        public A getReceiver() {
            return receiver;
        }

        public Duration getDuration() {
            return duration;
        }
    }
    
    private final class InternalEndpointScheduler implements EndpointScheduler {

        @Override
        public void scheduleMessage(Duration delay, Endpoint source, Endpoint destination, Object message) {
            Validate.isTrue(!delay.isNegative(), "Negative duration not allowed");
            
            events.add(new ScheduledMessageEvent(source, destination, message, lastWhen.plus(delay)));
        }

        @Override
        public void close() throws IOException {
            // do nothing
        }
    }
    
    private final class InternalEndpointDirectory implements EndpointDirectory<A> {

        @Override
        public Endpoint lookup(A id) {
            Validate.notNull(id);
            return new InternalEndpoint(id);
        }
        
    }
    
    private final class InternalEndpointIdentifier implements EndpointIdentifier<A> {

        @Override
        public A identify(Endpoint endpoint) {
            Validate.notNull(endpoint);
            
            ActorBundle bundle = actorLookupByEndpoint.get(endpoint);
            return bundle != null ? bundle.getName() : null;
        }
        
    }
}
