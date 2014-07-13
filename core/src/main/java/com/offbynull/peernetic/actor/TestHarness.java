package com.offbynull.peernetic.actor;

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

public final class TestHarness {

    private MessageDriver messageDriver;
    private PriorityQueue<Event> events;
    private Map<String, ActorBundle> actorLookupById;
    private Map<Endpoint, ActorBundle> actorLookupByEndpoint;
    private EndpointScheduler endpointScheduler;
    private EndpointDirectory<String> endpointDirectory;
    private EndpointIdentifier<String> endpointIdentifier;
    private Instant lastWhen;

    public TestHarness() {
        this(Instant.ofEpochMilli(0L), new BasicMessageDriver());
    }
    
    public TestHarness(Instant startTime) {
        this(startTime, new BasicMessageDriver());
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

    public void addActor(String name, Actor actor, Instant when) {
        Validate.notNull(name);
        Validate.notNull(actor);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to add actor event prior to current time");

        events.add(new JoinEvent(name, actor, when));
    }

    public void removeActor(String name, Instant when) {
        Validate.notNull(name);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(lastWhen), "Attempting to remove actor event prior to current time");

        events.add(new LeaveEvent(name, when));
    }
    
    public boolean hasMore() {
        return !events.isEmpty();
    }
    
    public Instant process() {
        Event event = events.poll();
        Validate.isTrue(event != null, "No events left to process");

        lastWhen = event.getWhen();

        if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            
            Object message = messageEvent.getMessage();
            String from = messageEvent.getFrom();
            String to = messageEvent.getTo();
            
            ActorBundle srcBundle = actorLookupById.get(from);
            ActorBundle dstBundle = actorLookupById.get(to);
            if (dstBundle != null) {
                Actor actor = dstBundle.getActor();
                Endpoint source = srcBundle != null ? srcBundle.getEndpoint() : new InternalEndpoint(from); // create fake if not exists
                Endpoint destination = dstBundle.getEndpoint();
                
                try {
                    actor.onStep(lastWhen, source, message);
                } catch (Exception e) {
                    // TODO: Log here

                    try {
                        actor.onStop(lastWhen);
                    } catch (Exception ex) {
                        // TODO: Log here
                    }

                    actorLookupById.remove(to);
                    actorLookupByEndpoint.remove(destination);
                }
            }
        } else if (event instanceof ScheduledMessageEvent) {
            ScheduledMessageEvent scheduledMessageEvent = (ScheduledMessageEvent) event;
            
            Object message = scheduledMessageEvent.getMessage();
            Endpoint source = scheduledMessageEvent.getSource();
            Endpoint destination = scheduledMessageEvent.getDestination();
            
            ActorBundle dstBundle = actorLookupByEndpoint.get(destination);
            if (dstBundle != null) {
                Actor actor = dstBundle.getActor();
                
                try {
                    actor.onStep(lastWhen, source, message);
                } catch (Exception e) {
                    // TODO: Log here

                    try {
                        actor.onStop(lastWhen);
                    } catch (Exception ex) {
                        // TODO: Log here
                    }

                    actorLookupById.remove(dstBundle.getName());
                    actorLookupByEndpoint.remove(destination);
                }
            }
        } else if (event instanceof JoinEvent) {
            JoinEvent joinEvent = (JoinEvent) event;

            String name = joinEvent.getName();
            Actor actor = joinEvent.getActor();
            InternalEndpoint endpoint = new InternalEndpoint(name);
            
            ActorBundle bundle = new ActorBundle(name, actor, endpoint);

            ActorBundle prevBundle = actorLookupById.putIfAbsent(name, bundle);
            Validate.isTrue(prevBundle == null, "Actor identifier already in use");
            actorLookupByEndpoint.put(endpoint, bundle);

            try {
                actor.onStart(lastWhen);
            } catch (Exception e) {
                // TODO: Log here
                
                try {
                    actor.onStop(lastWhen);
                } catch (Exception ex) {
                    // TODO: Log here
                }
                
                actorLookupById.remove(name);
                actorLookupByEndpoint.remove(endpoint);
            }
        } else if (event instanceof LeaveEvent) {
            LeaveEvent leaveEvent = (LeaveEvent) event;

            String name = leaveEvent.getName();

            ActorBundle bundle = actorLookupById.get(name);
            Validate.isTrue(bundle != null, "Actor identifier does not exist");
            
            try {
                bundle.getActor().onStop(lastWhen);
            } catch (Exception ex) {
                // TODO: Log here
            }
            
            actorLookupById.remove(name);
            actorLookupByEndpoint.remove(bundle.getEndpoint());
        } else {
            throw new IllegalStateException();
        }
        
        return lastWhen;
    }

    public Endpoint getEndpoint(String name) {
        ActorBundle bundle = actorLookupById.get(name);
        Validate.isTrue(bundle != null, "No actor found");
        
        return bundle.getEndpoint();
    }
    
    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public EndpointDirectory<String> getEndpointDirectory() {
        return endpointDirectory;
    }

    public EndpointIdentifier<String> getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public void scheduleFromNull(Duration duration, String name, Object message) {
        endpointScheduler.scheduleMessage(duration, NullEndpoint.INSTANCE, new InternalEndpoint(name), message);
    }
    
    private final class InternalEndpoint implements Endpoint {
        private String name;

        public InternalEndpoint(String name) {
            Validate.notNull(name);
            this.name = name;
        }
        
        @Override
        public void send(Endpoint source, Object message) {
            ActorBundle sourceActorBundle = actorLookupByEndpoint.get(source);
            
            List<MessageEnvelope> messageEnvelopes = messageDriver.onMessageSend(sourceActorBundle.getName(), name, message);
            messageEnvelopes.forEach(x -> {
                Validate.isTrue(!x.getDuration().isNegative(), "Negative duration not allowed");
                Instant when = lastWhen.plus(x.getDuration());
                events.add(new MessageEvent(x.getSender(), x.getReceiver(), x.getMessage(), when));
            });
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + Objects.hashCode(this.name);
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
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }
        
    }
    
    private static final class ActorBundle {
        private String name;
        private Actor actor;
        private Endpoint endpoint;

        public ActorBundle(String name, Actor actor, Endpoint endpoint) {
            Validate.notNull(name);
            Validate.notNull(actor);
            Validate.notNull(endpoint);
            
            this.name = name;
            this.actor = actor;
            this.endpoint = endpoint;
        }

        public String getName() {
            return name;
        }

        public Actor getActor() {
            return actor;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }
        
    }
    
    private static abstract class Event implements Comparable<Event> {

        private Instant when;

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

    private static final class MessageEvent extends Event {

        private String from;
        private String to;
        private Object message;

        public MessageEvent(String from, String to, Object message, Instant when) {
            super(when);
            this.from = from;
            this.to = to;
            this.message = message;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public Object getMessage() {
            return message;
        }

    }

    private static final class ScheduledMessageEvent extends Event {

        private Endpoint source;
        private Endpoint destination;
        private Object message;

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
    
    private static final class JoinEvent extends Event {

        private String name;
        private Actor actor;

        public JoinEvent(String name, Actor actor, Instant when) {
            super(when);
            this.name = name;
            this.actor = actor;
        }

        public String getName() {
            return name;
        }

        public Actor getActor() {
            return actor;
        }

    }

    private static final class LeaveEvent extends Event {

        private String name;

        public LeaveEvent(String name, Instant when) {
            super(when);
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    public interface MessageDriver {

        List<MessageEnvelope> onMessageSend(String sender, String receiver, Object message);
    }
    
    public static final class BasicMessageDriver implements MessageDriver {

        @Override
        public List<MessageEnvelope> onMessageSend(String sender, String receiver, Object message) {
            return Collections.singletonList(new MessageEnvelope(sender, receiver, message, Duration.ZERO));
        }
        
    }

    public static final class MessageEnvelope {

        private Object message;
        private String sender;
        private String receiver;
        private Duration duration;

        public MessageEnvelope(String sender, String receiver, Object message, Duration duration) {
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

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public Duration getDuration() {
            return duration;
        }
    }
    
    public final class InternalEndpointScheduler implements EndpointScheduler {

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
    
    public final class InternalEndpointDirectory implements EndpointDirectory<String> {

        @Override
        public Endpoint lookup(String id) {
            Validate.notNull(id);
            return new InternalEndpoint(id);
        }
        
    }
    
    public final class InternalEndpointIdentifier implements EndpointIdentifier<String> {

        @Override
        public String identify(Endpoint endpoint) {
            Validate.notNull(endpoint);
            
            ActorBundle bundle = actorLookupByEndpoint.get(endpoint);
            return bundle != null ? bundle.getName() : null;
        }
        
    }
}
