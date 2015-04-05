package com.offbynull.peernetic.core.test;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Actor;
import static com.offbynull.peernetic.core.actor.Actor.MANAGEMENT_ADDRESS;
import static com.offbynull.peernetic.core.actor.Actor.MANAGEMENT_PREFIX;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.Context.BatchedOutgoingMessage;
import com.offbynull.peernetic.core.actor.CoroutineActor;
import com.offbynull.peernetic.core.common.AddressUtils;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

public final class TestHarness {
    private final UnmodifiableMap<Class<? extends Event>, Consumer<Event>> eventHandlers;
    private final PriorityQueue<Event> events;
    private final String timerPrefix;
    private final Map<String, ActorBundle> actors;
    private Instant lastWhen;
    
    public TestHarness(String timerPrefix) {
        this(Instant.ofEpochMilli(0L), timerPrefix);
    }
    
    public TestHarness(Instant startTime, String timerPrefix) {
        Validate.notNull(startTime);
        Validate.notNull(timerPrefix);

        this.events = new PriorityQueue<>();
        this.timerPrefix = timerPrefix;
        this.actors = new HashMap<>();
        this.lastWhen = startTime;
        
        Map<Class<? extends Event>, Consumer<Event>> eventHandlers = new HashMap<>();
        eventHandlers.put(CustomEvent.class, this::handleCustomEvent);
        eventHandlers.put(JoinEvent.class, this::handleJoinEvent);
        eventHandlers.put(LeaveEvent.class, this::handleLeaveEvent);
        eventHandlers.put(MessageEvent.class, this::handleMessageEvent);
        
        this.eventHandlers =
                (UnmodifiableMap<Class<? extends Event>, Consumer<Event>>) UnmodifiableMap.unmodifiableMap(eventHandlers);
    }

    public void addCoroutineActor(String address, Coroutine coroutine, Duration timeOffset, Instant when, Object... primingMessages) {
        Validate.notNull(coroutine);
        addActor(address, new CoroutineActor(coroutine), timeOffset, when, primingMessages);
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

        events.add(new JoinEvent(address, actor, timeOffset, when, primingMessages));
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
        
        Consumer<Event> eventHandler = eventHandlers.get(event.getClass());
        Validate.validState(eventHandler != null);
        eventHandler.accept(event);
        
        return lastWhen;
    }
    
    private void handleCustomEvent(Event event) {
        CustomEvent customEvent = (CustomEvent) event;
        Runnable runnable = customEvent.getRunnable();
        runnable.run();
    }
    
    private void handleMessageEvent(Event event) {
        MessageEvent messageEvent = (MessageEvent) event;

        Object message = messageEvent.getMessage();
        String source = messageEvent.getSourceAddress();
        String destination = messageEvent.getDestinationAddress();

        ActorBundle actorBundle = findActor(destination);
        if (actorBundle == null) {
            // LOG WARNING
            return;
        }
        
        processMessages(actorBundle, source, destination, message);
    }
    
    private void handleJoinEvent(Event event) {
        JoinEvent joinEvent = (JoinEvent) event;

        String address = joinEvent.getAddress();
        Actor actor = joinEvent.getActor();
        Duration timeOffset = joinEvent.getTimeOffset();
        UnmodifiableList<Object> primingMessages = joinEvent.getPrimingMessages();

        validateActorAddressDoesNotConflict(address);

        ActorBundle newBundle = new ActorBundle(address, actor, timeOffset);
        actors.put(address, newBundle); // won't overwrite because validateActorAddress above will throw exception if it does

        processMessages(newBundle, MANAGEMENT_ADDRESS, MANAGEMENT_ADDRESS, primingMessages.toArray());        
    }
    
    private void handleLeaveEvent(Event event) {
        LeaveEvent leaveEvent = (LeaveEvent) event;

        String address = leaveEvent.getAddress();

        ActorBundle bundle = actors.remove(address);
        Validate.isTrue(bundle != null, "Actor identifier does not exist");
    }
    
    private void validateActorAddressDoesNotConflict(String address) {
        Validate.notNull(address);
        Validate.isTrue(
                !AddressUtils.isParent(timerPrefix, address),
                "Actor address {} conflicts with timer address {}", address, timerPrefix);
        Validate.isTrue(
                !AddressUtils.isParent(MANAGEMENT_PREFIX, address),
                "Actor address {} conflicts with management address {}", address, MANAGEMENT_PREFIX);
        ActorBundle conflictingActor = findActor(address);
        Validate.isTrue(conflictingActor == null,
                "Actor address {} conflicts with existing actor address {}", address, conflictingActor);
    }

    private ActorBundle findActor(String address) {
        Validate.notNull(address);

        List<String> splitAddress = Arrays.asList(AddressUtils.splitAddress(address));
        
        for (int i = 0; i <= splitAddress.size(); i++) {
            StringJoiner joiner = new StringJoiner(SEPARATOR);
            splitAddress.subList(0, i).forEach(x -> joiner.add(x));
            
            String testAddress = joiner.toString();
            ActorBundle actorBundle = actors.get(testAddress);
            if (actorBundle != null) {
                return actorBundle;
            }
        }
        
        return null;
    }
    
    private void processMessages(ActorBundle actorBundle, String source, String destination, Object ... messages) {
        Validate.notNull(actorBundle);
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(messages);
        Validate.noNullElements(messages);
        
        Actor actor = actorBundle.getActor();
        String address = actorBundle.getAddress();
        Context context = actorBundle.getContext();
        Instant localActorTime = lastWhen.plus(actorBundle.getTimeOffset());
        
        try {
            for (Object message : messages) {
                context.setTime(localActorTime);
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
                    
                    if (AddressUtils.isParent(timerPrefix, newDestination)) {
                        // Message for timer. Add in the RESPONSE FROM the timer.
                        try {
                            String durationStr = AddressUtils.getAddressElement(newDestination, 1);
                            Duration duration = Duration.ofMillis(Long.parseLong(durationStr));
                            Validate.isTrue(!duration.isNegative());

                            events.add(new MessageEvent(newDestination, newSource, newMessage, lastWhen.plus(duration)));
                        } catch (Exception e) {
                            // TODO log here. nothing else, technically if the timer gets bad suffixes or anything like that it'll ignore
                        }
                    } else {
                        // Message for other actor. Add in the message.
                        events.add(new MessageEvent(newSource, newDestination, newMessage, lastWhen));
                    }
                }
            }
        } catch (Exception e) {
            actors.remove(address);
        }
    }
}
