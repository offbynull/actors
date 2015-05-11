/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.simulation;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.SourceContext;
import com.offbynull.peernetic.core.actor.BatchedOutgoingMessage;
import com.offbynull.peernetic.core.actor.CoroutineActor;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import static com.offbynull.peernetic.core.shuttle.AddressUtils.SEPARATOR;
import com.offbynull.peernetic.core.simulation.MessageSource.SourceMessage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

public final class Simulator {
    private final UnmodifiableMap<Class<? extends Event>, Consumer<Event>> eventHandlers;
    private final PriorityQueue<Event> events;
    private final Map<String, Holder> holders;
    private final Set<MessageSink> sinks;
    private final Set<MessageSource> sources;
    private final ActorBehaviourDriver actorDurationCalculator;
    private final MessageBehaviourDriver messageDurationCalculator;
    private Instant currentTime;
    private long nextSequenceNumber; // Each time an event created and added to the events collection, this sequence number is incremented
                                     // and used for the events sequence number. The sequence number is used to properly order order events
                                     // that trigger at the same time. That is, if two events trigger at the same time, they'll be returned
                                     // in the order they were added.
    
    public Simulator() {
        this(Instant.ofEpochMilli(0L));
    }
    
    public Simulator(Instant startTime) {
        this(startTime, new SimpleActorBehaviourDriver(), new SimpleMessageBehaviourDriver());
    }
    
    public Simulator(
            Instant startTime,
            ActorBehaviourDriver actorDurationCalculator,
            MessageBehaviourDriver messageDurationCalculator) {
        Validate.notNull(startTime);
        Validate.notNull(actorDurationCalculator);
        Validate.notNull(messageDurationCalculator);

        this.events = new PriorityQueue<>();
        this.holders = new HashMap<>();
        this.currentTime = startTime;
        
        Map<Class<? extends Event>, Consumer<Event>> eventHandlers = new HashMap<>();
        eventHandlers.put(CustomEvent.class, this::handleCustomEvent);
        eventHandlers.put(AddActorEvent.class, this::handleAddActorEvent);
        eventHandlers.put(RemoveActorEvent.class, this::handleRemoveActorEvent);
        eventHandlers.put(AddTimerEvent.class, this::handleAddTimerEvent);
        eventHandlers.put(RemoveTimerEvent.class, this::handleRemoveTimerEvent);
        eventHandlers.put(MessageEvent.class, this::handleMessageEvent);
        eventHandlers.put(AddMessageSourceEvent.class, this::handleAddMessageSourceEvent);
        eventHandlers.put(PullFromMessageSourceEvent.class, this::handlePullFromMessageSourceEvent);
        eventHandlers.put(RemoveMessageSourceEvent.class, this::handleRemoveMessageSourceEvent);
        eventHandlers.put(AddMessageSinkEvent.class, this::handleAddMessageSinkEvent);
        eventHandlers.put(RemoveMessageSinkEvent.class, this::handleRemoveMessageSinkEvent);
        
        this.eventHandlers =
                (UnmodifiableMap<Class<? extends Event>, Consumer<Event>>) UnmodifiableMap.unmodifiableMap(eventHandlers);
        this.actorDurationCalculator = actorDurationCalculator;
        this.messageDurationCalculator = messageDurationCalculator;
        
        this.sinks = new HashSet<>();
        this.sources = new HashSet<>();
    }
    
    public void addMessageSink(MessageSink sink, Instant when) {
        Validate.notNull(sink);
        Validate.isTrue(!sinks.contains(sink));
        
        events.add(new AddMessageSinkEvent(sink, when, nextSequenceNumber++));
    }

    public void addMessageSource(MessageSource source, Instant when) {
        Validate.notNull(source);
        Validate.isTrue(!sources.contains(source));
        
        events.add(new AddMessageSourceEvent(source, when, nextSequenceNumber++));
    }
    
    public void removeSink(MessageSink sink, Instant when) {
        Validate.notNull(sink);
        Validate.isTrue(!sinks.contains(sink));
        
        events.add(new RemoveMessageSinkEvent(sink, when, nextSequenceNumber++));
    }

    public void removeSource(MessageSource source, Instant when) {
        Validate.notNull(source);
        Validate.isTrue(!sources.contains(source));
        
        events.add(new RemoveMessageSourceEvent(source, when, nextSequenceNumber++));
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

        events.add(new AddActorEvent(address, actor, timeOffset, when, nextSequenceNumber++, primingMessages));
    }

    public void removeActor(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to remove actor event prior to current time");

        events.add(new RemoveActorEvent(address, when, nextSequenceNumber++));
    }

    public void addTimer(String address, long granularity, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(granularity >= 0, "Negative granularity not allowed");
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add timer event prior to current time");

        events.add(new AddTimerEvent(address, granularity, when, nextSequenceNumber++));
    }

    public void removeTimer(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to remove timer event prior to current time");

        events.add(new RemoveTimerEvent(address, when, nextSequenceNumber++));
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
    
    private void handleAddActorEvent(Event event) {
        AddActorEvent joinEvent = (AddActorEvent) event;

        String address = joinEvent.getAddress();
        Actor actor = joinEvent.getActor();
        Duration timeOffset = joinEvent.getTimeOffset();
        UnmodifiableList<Object> primingMessages = joinEvent.getPrimingMessages();

        validateAddressDoesNotConflict(address);

        ActorHolder newHolder = new ActorHolder(address, actor, timeOffset, currentTime);
        holders.put(address, newHolder); // won't overwrite because validateAddresDoesNotConflict above will throw exception if it does

        for (Object message : primingMessages) {
            queueMessage(true, address, address, message, Duration.ZERO, 0L);
        }
    }
    
    private void handleRemoveActorEvent(Event event) {
        RemoveActorEvent removeActorEvent = (RemoveActorEvent) event;

        String address = removeActorEvent.getAddress();

        Holder holder = holders.remove(address);
        Validate.isTrue(holder != null, "Address does not exist");
        if (!(holder instanceof ActorHolder)) {
            // If the holder removed was not an actor holder, add it back in and throw an exception
            holders.put(address, holder);
            throw new IllegalArgumentException("Address not an actor");
        }
    }

    private void handleAddTimerEvent(Event event) {
        AddTimerEvent addTimerEvent = (AddTimerEvent) event;

        String address = addTimerEvent.getAddress();
        long granularity = addTimerEvent.getGranularity();

        validateAddressDoesNotConflict(address);

        TimerHolder newHolder = new TimerHolder(address, granularity);
        holders.put(address, newHolder); // won't overwrite because validateAddresDoesNotConflict above will throw exception if it does
    }
    
    private void handleRemoveTimerEvent(Event event) {
        RemoveTimerEvent removeTimerEvent = (RemoveTimerEvent) event;

        String address = removeTimerEvent.getAddress();

        Holder holder = holders.remove(address);
        Validate.isTrue(holder != null, "Address does not exist");
        if (!(holder instanceof TimerHolder)) {
            // If the holder removed was not a timer holder, add it back in and throw an exception
            holders.put(address, holder);
            throw new IllegalArgumentException("Address not a timer");
        }
        
        // In addition to removing the timer, we need to remove all messages queued by the timer. The timer has been removed, which means
        // that it'll never get a chance to send these messages out.
        Iterator<Event> eventsIt = events.iterator();
        while (eventsIt.hasNext()) {
            Event pendingEvent = eventsIt.next();
            if (pendingEvent instanceof MessageEvent) {
                MessageEvent messageEvent = (MessageEvent) pendingEvent;
                if (AddressUtils.isPrefix(address, messageEvent.getSourceAddress())) {
                    eventsIt.remove();
                }
            }
        }
    }
    
    private void handleAddMessageSourceEvent(Event event) {
        AddMessageSourceEvent addMessageSourceEvent = (AddMessageSourceEvent) event;
        
        MessageSource source = addMessageSourceEvent.getMessageSource();
        Validate.isTrue(!sources.contains(source));
        sources.add(source);
        
        // Queue an immediate pull
        events.add(new PullFromMessageSourceEvent(source, currentTime, nextSequenceNumber++));
    }

    private void handlePullFromMessageSourceEvent(Event event) {
        PullFromMessageSourceEvent pullFromMessageSourceEvent = (PullFromMessageSourceEvent) event;
        
        MessageSource source = pullFromMessageSourceEvent.getMessageSource();
        if (!sources.contains(source)) {
            // The MessageSource was removed, so ignore this pull event.
            return;
        }
        
        // Read the message from the message source
        SourceMessage sourceMessage;
        try {
            sourceMessage = source.readNextMessage();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        
        if (sourceMessage == null) {
            // We've reached the end of the message source. Remove the message source and return.
            sources.remove(source);
            return;
        }
        
        // Queue message
        queueMessage(false,
                sourceMessage.getSource(),
                sourceMessage.getDestination(),
                sourceMessage.getMessage(),
                sourceMessage.getDuration(),
                0L);
        
        // Queue another pull (read the next message) once the message arrives
        Instant nextPullTime = currentTime.plus(sourceMessage.getDuration());
        events.add(new PullFromMessageSourceEvent(source, nextPullTime, nextSequenceNumber++));
    }
    
    private void handleRemoveMessageSourceEvent(Event event) {
        RemoveMessageSourceEvent removeMessageSourceEvent = (RemoveMessageSourceEvent) event;
        
        MessageSource source = removeMessageSourceEvent.getMessageSource();
        Validate.isTrue(sources.remove(source));
    }
    
    private void handleAddMessageSinkEvent(Event event) {
        AddMessageSinkEvent addMessageSinkEvent = (AddMessageSinkEvent) event;

        MessageSink sink = addMessageSinkEvent.getMessageSink();
        Validate.isTrue(!sinks.contains(sink));
        sinks.add(sink);
    }
    
    private void handleRemoveMessageSinkEvent(Event event) {
        RemoveMessageSinkEvent removeMessageSinkEvent = (RemoveMessageSinkEvent) event;
        
        MessageSink sink = removeMessageSinkEvent.getMessageSink();
        Validate.isTrue(sinks.remove(sink));
    }
    
    private void validateAddressDoesNotConflict(String address) {
        Validate.notNull(address);
        Holder conflictingHolder = findHolder(address);
        Validate.isTrue(conflictingHolder == null,
                "Address {} conflicts with existing address {}", address, conflictingHolder);
    }

    private Holder findHolder(String address) {
        Validate.notNull(address);

        List<String> splitAddress = Arrays.asList(AddressUtils.splitAddress(address));
        
        for (int i = 0; i <= splitAddress.size(); i++) {
            StringJoiner joiner = new StringJoiner(SEPARATOR);
            splitAddress.subList(0, i).forEach(x -> joiner.add(x));
            
            String testAddress = joiner.toString();
            Holder actorHolder = holders.get(testAddress);
            if (actorHolder != null) {
                return actorHolder;
            }
        }
        
        return null;
    }
    
    private void queueMessage(
            boolean sourceMustExistFlag,
            String sourceAddress,
            String destinationAddress,
            Object message,
            Duration scheduledDuration,
            long granularity) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notNull(scheduledDuration);
        Validate.notNull(granularity);
        Validate.isTrue(!scheduledDuration.isNegative());
        Validate.isTrue(granularity >= 0L);
        
        // Source must exist.
        //
        // Destination doesn't have to exist. It's perfectly valid to send a message to an actor that doesn't exist yet but may have come in
        // to existance exist by the time the message arrives.
        if (sourceMustExistFlag) {
            // Only check to see if source actor exists if we the flag is set to do so
            Validate.isTrue(findHolder(sourceAddress) != null);
        }
        
        Instant arriveTime = currentTime;
        
        // Add the amount of time it takes the message to arrive for processing. For messages sent between local gateways/actors, duration
        // should be zero (or close to zero).
        Duration messageDuration = messageDurationCalculator.calculateDuration(sourceAddress, destinationAddress, message);
        Validate.isTrue(!messageDuration.isNegative()); // sanity check here, make sure it isn't negative
        
        arriveTime = arriveTime.plus(messageDuration);
        
        // Message may be scheduled by the timer to run at a certain delay. If it is, add that scheduled delay to the arrival time.
        arriveTime = arriveTime.plus(scheduledDuration);
        
        // That timer may also be set to run at some time granularity. Apply that granularity here (if applicable).
        if (granularity != 0L) {
            long origTime = arriveTime.toEpochMilli();
            long remaining = arriveTime.toEpochMilli() % granularity;
            long time = origTime + remaining;
            arriveTime = Instant.ofEpochMilli(time);
        }
        
        // Earliest possible step time is the minimum point in time which the destination actor can have its onStep called again. Why is
        // this here? because the last onStep call took some amount of time to execute. That duration of time has already elapsed
        // (technically). It makes sense no sense to have a message arrive at that actor before then. So, we make sure here that it doesn't
        // arrive before then.
        Holder destHolder = findHolder(destinationAddress);
        if (destHolder != null && destHolder instanceof ActorHolder) { // destHolder may be null, see comment above about destination not
                                                                       // having to exist
            Instant nextAvailableStepTime = ((ActorHolder) destHolder).getEarliestPossibleOnStepTime();
            if (arriveTime.isBefore(nextAvailableStepTime)) {
                arriveTime = nextAvailableStepTime;
            }
        }
        
        events.add(new MessageEvent(sourceAddress, destinationAddress, message, arriveTime, nextSequenceNumber++));
    }
    
    private void processMessage(MessageEvent messageEvent) {
        Validate.notNull(messageEvent);
        
        String source = messageEvent.getSourceAddress();
        String destination = messageEvent.getDestinationAddress();
        Object message = messageEvent.getMessage();
        
        // Get destination
        Holder holder = findHolder(destination);

        // If destination doesn't exist, log a warning and move on. In the real world if the destination doesn't exist or is otherwise
        // unreachable, the same thing happens -- message is sent to some destination but the system doesn't really care if it arrives or
        // not.
        if (holder == null) {
            // TODO log here
            return;
        }

        if (holder instanceof TimerHolder) {
            processMessageToTimer((TimerHolder) holder, destination, source, message);
        } else if (holder instanceof ActorHolder) {
            processMessageToActor((ActorHolder) holder, destination, source, message);
        } else {
            throw new IllegalStateException();
        }
    }

    private void processMessageToTimer(TimerHolder destHolder, String destination, String source, Object message) {
        // If this is a timer messages, bounce it back to the source -- scheduled to arrive after the specified duration
        Duration timerDuration;
        try {
            String durationStr = AddressUtils.getElement(destination, 1);
            timerDuration = Duration.ofMillis(Long.parseLong(durationStr));
            Validate.isTrue(!timerDuration.isNegative());
        } catch (Exception e) {
            // TODO log here. nothing else, technically if the timer gets an unparsable duration it'll ignore the message
            return;
        }
        queueMessage(true, destination, source, message, timerDuration, destHolder.getGranularity());
    }

    private void processMessageToActor(ActorHolder destHolder, String destination, String source, Object message) {
        // Earliest possible step time is the minimum point in time which the destination actor can have its onStep called again. That is,
        // the message being processed must be >= to the earliest possible step time. Otherwise, something has gone wrong. This is a sanity
        // check.
        Instant minTime = destHolder.getEarliestPossibleOnStepTime();
        Validate.isTrue(!currentTime.isBefore(minTime));


        // Set up values for calling onStep, and then call onStep. If onStep throws an exception, then log and remove the actor from the
        // test harness. In the real world, if an actor throws an exception, it'll get removed from the list of actors assigned to that
        // ActorThread/ActorRunnable and no more execution is done on it.
        Actor actor = destHolder.getActor();
        String address = destHolder.getAddress();
        SourceContext context = destHolder.getContext();
        Instant localActorTime = currentTime.plus(destHolder.getTimeOffset()); // This is the time as it appears to the actor. Clocks
                                                                               // between different machines are never going to be entirely
                                                                               // in sync. So, one actor may technically have a different
                                                                               // local time than another (because they may be running on
                                                                               // different machines).

        context.setTime(localActorTime);
        context.setSource(source);
        context.setDestination(destination);
        context.setSelf(address);
        context.setIncomingMessage(message);

        for (MessageSink sink : sinks) {
            try {
                sink.writeNextMessage(source, destination, localActorTime, message);
            } catch (IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        }

        Duration realExecDuration;
        boolean stopped;
        Instant execStartTime = Instant.now();
        try {
            stopped = !actor.onStep(context.toNormalContext());
        } catch (Exception e) {
            stopped = true;
        }
        Instant execEndTime = Instant.now();
        realExecDuration = Duration.between(execStartTime, execEndTime);

        if (stopped) {
            // Actor stopped or crashed. Remove it from the list of actors and stop processing.
            holders.remove(address);
            return;
        }

        for (BatchedOutgoingMessage batchedOutMsg : context.copyAndClearOutgoingMessages()) {
            String outSrc = AddressUtils.parentize(address, batchedOutMsg.getSourceId());
            queueMessage(
                    true,
                    outSrc,
                    batchedOutMsg.getDestination(),
                    batchedOutMsg.getMessage(),
                    Duration.ZERO,
                    0L);
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
        destHolder.setEarliestPossibleOnStepTime(earliestPossibleOnStepTime);

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
