package com.offbynull.peernetic.core.test;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.CoroutineActor;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.SEPARATOR;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
    private final ActorBehaviourDriver actorDurationCalculator;
    private final MessageBehaviourDriver messageDurationCalculator;
    private Instant currentTime;
    private long nextSequenceNumber; // Each time an event created and added to the events collection, this sequence number is incremented
                                     // and used for the events sequence number. The sequence number is used to properly order order events
                                     // that trigger at the same time. That is, if two events trigger at the same time, they'll be returned
                                     // in the order they were added.
    
    public TestHarness(String timerPrefix) {
        this(Instant.ofEpochMilli(0L), timerPrefix);
    }
    
    public TestHarness(Instant startTime, String timerPrefix) {
        this(startTime, timerPrefix, new SimpleActorBehaviourDriver(), new SimpleMessageBehaviourDriver());
    }
    
    public TestHarness(Instant startTime, String timerPrefix, ActorBehaviourDriver actorDurationCalculator,
            MessageBehaviourDriver messageDurationCalculator) {
        Validate.notNull(startTime);
        Validate.notNull(timerPrefix);
        Validate.notNull(actorDurationCalculator);
        Validate.notNull(messageDurationCalculator);

        this.events = new PriorityQueue<>();
        this.timerPrefix = timerPrefix;
        this.actors = new HashMap<>();
        this.currentTime = startTime;
        
        Map<Class<? extends Event>, Consumer<Event>> eventHandlers = new HashMap<>();
        eventHandlers.put(CustomEvent.class, this::handleCustomEvent);
        eventHandlers.put(JoinEvent.class, this::handleJoinEvent);
        eventHandlers.put(LeaveEvent.class, this::handleLeaveEvent);
        eventHandlers.put(MessageEvent.class, this::handleMessageEvent);
        
        this.eventHandlers =
                (UnmodifiableMap<Class<? extends Event>, Consumer<Event>>) UnmodifiableMap.unmodifiableMap(eventHandlers);
        this.actorDurationCalculator = actorDurationCalculator;
        this.messageDurationCalculator = messageDurationCalculator;
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
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add actor event prior to current time");
        Validate.isTrue(!timeOffset.isNegative(), "Negative time offset not allowed");

        events.add(new JoinEvent(address, actor, timeOffset, when, nextSequenceNumber++, primingMessages));
    }

    public void removeActor(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to remove actor event prior to current time");

        events.add(new LeaveEvent(address, when, nextSequenceNumber++));
    }

    public void addCustom(Runnable runnable, Instant when) {
        Validate.notNull(runnable);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add custom event prior to current time");

        events.add(new CustomEvent(runnable, when, nextSequenceNumber++));
    }

    public boolean hasMore() {
        return !events.isEmpty();
    }
    
    public Instant process() {
        Event event = events.poll();
        Validate.isTrue(event != null, "No events left to process");

        currentTime = event.getTriggerTime();
        
        Consumer<Event> eventHandler = eventHandlers.get(event.getClass());
        Validate.validState(eventHandler != null);
        eventHandler.accept(event);
        
        return currentTime;
    }
    
    private void handleCustomEvent(Event event) {
        CustomEvent customEvent = (CustomEvent) event;
        Runnable runnable = customEvent.getRunnable();
        runnable.run();
    }
    
    private void handleMessageEvent(Event event) {
        MessageEvent messageEvent = (MessageEvent) event;        
        processMessage(messageEvent);
    }
    
    private void handleJoinEvent(Event event) {
        JoinEvent joinEvent = (JoinEvent) event;

        String address = joinEvent.getAddress();
        Actor actor = joinEvent.getActor();
        Duration timeOffset = joinEvent.getTimeOffset();
        UnmodifiableList<Object> primingMessages = joinEvent.getPrimingMessages();

        validateActorAddressDoesNotConflict(address);

        ActorBundle newBundle = new ActorBundle(address, actor, timeOffset, currentTime);
        actors.put(address, newBundle); // won't overwrite because validateActorAddress above will throw exception if it does

        for (Object message : primingMessages) {
            queueMessage(address, address, message, Duration.ZERO);
        }
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
    
    private void queueMessage(String source, String destination, Object message, Duration scheduledDuration) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.notNull(scheduledDuration);
        
        // Source must exist.
        //
        // Destination doesn't have to exist. It's perfectly valid to send a message to an actor that doesn't exist yet but may have come in
        // to existance exist by the time the message arrives.
        Validate.isTrue(findActor(source) != null);
        
        Instant arriveTime = currentTime;
        
        // Add the amount of time it takes the message to arrive for processing. For messages sent between local gateways/actors, duration
        // should be zero (or close to zero).
        Duration messageDuration = messageDurationCalculator.calculateDuration(source, destination, message);
        Validate.isTrue(!messageDuration.isNegative()); // sanity check here, make sure it isn't negative
        
        arriveTime = arriveTime.plus(messageDuration);
        
        // Message may be scheduled by the timer to run at a certain delay. If it is, add that scheduled delay to the arrival time.
        arriveTime = arriveTime.plus(scheduledDuration);
        
        // Earliest possible step time is the minimum point in time which the destination actor can have its onStep called again. Why is
        // this here? because the last onStep call took some amount of time to execute. That duration of time has already elapsed
        // (technically). It makes sense no sense to have a message arrive at that actor before then. So, we make sure here that it doesn't
        // arrive before then.
        ActorBundle destBundle = findActor(destination);
        if (destBundle != null) { //destBundle may be null, see comment above about destination not having to exist
            Instant nextAvailableStepTime = destBundle.getEarliestPossibleOnStepTime();
            if (arriveTime.isBefore(nextAvailableStepTime)) {
                arriveTime = nextAvailableStepTime;
            }
        }
        
        events.add(new MessageEvent(source, destination, message, arriveTime, nextSequenceNumber++));
    }
    
    private void processMessage(MessageEvent messageEvent) {
        Validate.notNull(messageEvent);
        
        String source = messageEvent.getSourceAddress();
        String destination = messageEvent.getDestinationAddress();
        Object message = messageEvent.getMessage();
        
        // Special case: If this is a timer messages, bounce it back to the source -- scheduled to arrive after the specified duration
        if (AddressUtils.isParent(timerPrefix, destination)) {
            Duration timerDuration;
            try {
                String durationStr = AddressUtils.getAddressElement(destination, 1);
                timerDuration = Duration.ofMillis(Long.parseLong(durationStr));
                Validate.isTrue(!timerDuration.isNegative());
            } catch (Exception e) {
                // TODO log here. nothing else, technically if the timer gets an unparsable duration it'll ignore the message
                return;
            }
            
            queueMessage(destination, source, message, timerDuration);
            return;
        }
        
        
        // Get destination
        ActorBundle destBundle = findActor(destination);
        
        // If destination doesn't exist, log a warning and move on. In the real world if the destination doesn't exist or is otherwise
        // unreachable, the same thing happens -- message is sent to some destination but the system doesn't really care if it arrives or
        // not.
        if (destBundle == null) {
            // TODO log here
            return;
        }
        
        // Earliest possible step time is the minimum point in time which the destination actor can have its onStep called again. That is,
        // the message being processed must be >= to the earliest possible step time. Otherwise, something has gone wrong. This is a sanity
        // check.
        Instant minTime = destBundle.getEarliestPossibleOnStepTime();
        Validate.isTrue(!currentTime.isBefore(minTime));
        
        
        // Set up values for calling onStep, and then call onStep. If onStep throws an exception, then log and remove the actor from the
        // test harness. In the real world, if an actor throws an exception, it'll get removed from the list of actors assigned to that
        // ActorThread/ActorRunnable and no more execution is done on it.
        Actor actor = destBundle.getActor();
        String address = destBundle.getAddress();
        Context context = destBundle.getContext();
        Instant localActorTime = currentTime.plus(destBundle.getTimeOffset()); // This is the time as it appears to the actor. Clocks
                                                                               // between different machines are never going to be entirely
                                                                               // in sync. So, one actor may technically have a different
                                                                               // local time than another (because they may be running on
                                                                               // different machines).
        
        context.setTime(localActorTime);
        context.setSource(source);
        context.setDestination(destination);
        context.setSelf(address);
        context.setIncomingMessage(message);
        
        Duration realExecDuration;
        try {
            Instant execStartTime = Instant.now();
            actor.onStep(context);
            Instant execEndTime = Instant.now();
            realExecDuration = Duration.between(execStartTime, execEndTime);
        } catch (Exception e) {
            // Actor crashed. Remove it from the list of actors and stop processing.
            actors.remove(address);
            return;
        }
        
        // We've finished calling onStep(). Next, add the amount of time it took to do the processing of the message by onStep. This is a
        // calculated value. We have the real execution time, and we pass that in as a hint to the interface that does the calculations, but
        // ultimately the interface can specify whatever duration it wants (provided that it isn't negative).
        //
        // Add the execution duration to the current time and set it as the actor's next possible step time. There's no way we can call
        // onStep() before this time. Otherwise we'd be going back in time.
        if (realExecDuration.isNegative()) { // System clock may be wonky, so make sure exec duration doesn't come out as negative!
            realExecDuration = Duration.ZERO;
        }
        Duration execDuration = actorDurationCalculator.calculateDuration(address, message, realExecDuration);
        Validate.isTrue(!execDuration.isNegative()); // sanity check here, make sure calculated duration it isn't negative
        
        Instant earliestPossibleOnStepTime = currentTime.plus(execDuration);
        destBundle.setEarliestPossibleOnStepTime(earliestPossibleOnStepTime);
        
        // Go through any messages queued to arrive at this actor before earliest possible onstep time. Reschedule each of those messages
        // such that they arrive at earliest possible on step time. Like the comment above says, it wouldn't make sense to call onStep()
        // before this time. We'd be going back in time if we did.
        List<MessageEvent> messageEventsToReschedule = new LinkedList<>();
        Iterator<Event> it = events.iterator();
        while (it.hasNext()) {
            Event event = it.next();
            
            // Events queue is ordered. If the event we're looking at right now is >= to the earliest possible onstep time, it means that
            // this event and all events after it don't need to be rescheduled, so stop checking here.
            if (!earliestPossibleOnStepTime.isBefore(event.getTriggerTime())) {
                break;
            }
            
            // We only care about MessageEvents because they're the ones that trigger calls to onStep(). If the event is a MessageEvent and
            // the destination of that event is this actor, remove it from the events queue and add it to a temporary collection. All items
            // in this temporary collection are going to be rescheduled such that they trigger at earliestPossibleOnStepTime and re-added
            // to the events queue.
            if (event instanceof MessageEvent) {
                MessageEvent pendingMessageEvent = (MessageEvent) event;
                if (pendingMessageEvent.getDestinationAddress().equals(address)) {
                    messageEventsToReschedule.add(pendingMessageEvent);
                    it.remove();
                }
            }
        }
        
        for (MessageEvent pendingMessageEvent : messageEventsToReschedule) {
            MessageEvent rescheduledMessageEvent = new MessageEvent(
                    pendingMessageEvent.getSourceAddress(),
                    pendingMessageEvent.getDestinationAddress(),
                    pendingMessageEvent.getMessage(),
                    pendingMessageEvent.getTriggerTime(),
                    nextSequenceNumber++);
            events.add(rescheduledMessageEvent);
        }
    }
}