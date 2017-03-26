/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.simulator;

import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.core.actor.SourceContext;
import com.offbynull.peernetic.core.actor.BatchedOutgoingMessage;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.simulator.MessageSource.SourceMessage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simulation environment for actors.
 * <p>
 * The main benefit to testing your actors using this class versus using {@link ActorRunner} is that this class is designed to run actors
 * "faster than real-time". That is, most actors tend to spend a majority of their time waiting for messages to arrive. This class exploits
 * that fact by skipping the actual waiting but giving actors the impression that the waits actually occurred.
 * <p>
 * The downside to this is that you cannot interface with any {@link Gateway}s. The reason for this is that gateway implementations may
 * be executing code that blocks, which means that the logic used by this class to mock-out blocking/waiting no longer applies. Instead,
 * mock functionality is provided to replace {@link TimerGateway}. If your actor relies on any other gateway implementations, you'll need to
 * mock those out yourself as custom actor implementations.
 * <p>
 * The following example creates a simulation environment with a coroutine actor that sends a delayed message to itself.
 * <pre>
 * Coroutine tester = (cnt) -&gt; {
     Context ctx = (Context) cnt.getContext();
 
     // Normally, actors shouldn't be logging to System.out or doing any other IO. They're here for demonstrative purposes.
     System.out.println("Sending out at " + ctx.time());
     String timerPrefix = ctx.in();
     ctx.out(timerPrefix + ":2000", 0);
     cnt.suspend();
     System.out.println("Got response at " + ctx.time());
 };
 
 Simulator testHarness = new Simulator();
 testHarness.addTimer("timer", 0L, Instant.ofEpochMilli(0L));
 testHarness.addActor("local", tester, Duration.ZERO, Instant.ofEpochMilli(0L), "timer");
 
 while (testHarness.hasMore()) {
     testHarness.process();
 }
 </pre>
 * Other important things to note when running your actors in a simulation:
 * <ol>
 * <li><b>Actors are added in to the simulator as-is.</b> When running actors in a real environment, you would normally run those actors in
 * one or more {@link ActorRunner}s and potentially bind their {@link Shuttle}s between each other and related {@link Gateway}s. In
 * contrast, the simulator has no concept of a container for actors (e.g. anything similar to {@link ActorRunner}) and no concept of a
 * explicitly binding between those containers (e.g. anything similar to {@link Shuttle}). Instead, actors are directly added to the
 * simulator and all addresses present in the simulator can send to and receive from each other.</li>
 * <li><b>Messages are passed between actors and (mocked) gateways instantly,</b> meaning that you cannot use the simulator to simulate
 * message delays (e.g. simulating a system under heavy load). However, delays caused by the execution of an actor can be simulated. See
 * {@link ActorDurationCalculator} and
 * {@link #Simulator(java.time.Instant, com.offbynull.peernetic.core.simulator.ActorDurationCalculator) } for more detail.</li>
 * <li><b>The time one actor takes to process an incoming message has no effect on the other actors in the simulation.</b> For example,
 * imagine that {@code actor1} and {@code actor2} both receive a message at the same time. {@code actor1} processes its message first and
 * takes 5 milliseconds to do so. {@code actor2} then processes its message <u>from the same point in time</u> (without the 5 milliseconds
 * tacked on), as if it was running in parallel with {@code actor1} vs after {@code actor1}. One way to think about this is that all actors
 * in the simulator run in parallel, as if each individual actor runs in its own actor runner.</li>
 * <li><b>Timer's have exact precision.</b> This will almost always never be the case when you run in a real environment. The precision of
 * a {@link TimerGateway} is dependent on the OS/platform and the current load on the system.</li>
 * <li><b>Garbage collection pauses do not occur.</b> Remember that the simulator is is essentially mocking out time. As such, pauses caused
 * by the JVM running garbage collection are not reflected on to actors running in the simulation.</li>
 * </ol>
 * @author Kasra Faghihi
 */
public final class Simulator {
    
    // NOTE that this is a simulator, not an emulator. There was a (partially implemented) idea at one point to bring this closer to being
    // an emulator. Here are the things that were implementer or were going to be implemented:
    //
    // 1. Timer granularity -- You could set the granularity at which a timer operates. For example, windows defaults to 15ms/16ms
    //    timer granularity, which could potentially go lower based on system time.
    // 2. Send duration -- When a message is sent out, it sits for a while before being sent out. If the sending actor is removed during
    //    that time, the messge is also removed. This is to simulate a sender under load. It can also be used to simulate out of order
    //    message sending (but networking mock actor currently provides this functionality).
    // 3. Recv duration -- When a message arrives, it sits for a while before going in to the actor for processing. If the recving actor is
    //    removed during that time, the message is also removed. This is to simulate a recver under load (backed up message queue). It can
    //    also be used to simulate out of order message recving (but networking mock actor currently provides this functionality).
    // 4. Actor container -- Simulate multiple actors in an ActorRunner. For example, if an actor sits in the same container as 5 other
    //    actors and gets an incoming message that takes 5 seconds to process, those 5 other actors will also be delayed by 5 seconds,
    //    because technically they're all running as if they're "in the same thread". In addition, simulate linking the containers the
    //    same way that shuttles are used to link together actors and gateways.
    // 5. Garbage collection pasues -- Simulat pausese, either from garbage collection or some other event (e.g. computer goes in to
    //    hibernation mode and comes back up?).
    // 6. Actor time offsets -- This is currently implemented.
    //
    // It's possible to do all of the above items in this class, but the class will become much more convoluted. In addition to that, there
    // will need to be a lot more unit tests. Performance will likely also suffer.
    //
    // What is here now reaches the "good enough" bar, at least for now.
    
    private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);
    
    private final UnmodifiableMap<Class<? extends Event>, Consumer<Event>> eventHandlers;
    private final PriorityQueue<Event> events;
    private final Map<Address, Holder> holders;
    private final Set<MessageSink> sinks;
    private final Set<MessageSource> sources;
    private final ActorDurationCalculator actorDurationCalculator;
    private Instant currentTime;
    private long nextSequenceNumber; // Each time an event created and added to the events collection, this sequence number is incremented
                                     // and used for the events sequence number. The sequence number is used to properly order order events
                                     // that trigger at the same time. That is, if two events trigger at the same time, they'll be returned
                                     // in the order they were added.
    
    /**
     * Constructs a {@link Simulator} object which is set to run it's simulation as if it's {@code 1970-01-01T00:00:00Z}. Equivalent to
     * calling {@code new Simulator(Instant.ofEpochMilli(0L))}.
     */
    public Simulator() {
        this(Instant.ofEpochMilli(0L));
    }
    
    /**
     * Constructs a {@link Simulator} object which is set to run it's simulation as if from some specific point in time. Equivalent to
     * calling {@code new Simulator(startTime, new SimpleActorDurationCalculator())}.
     * @param startTime start time of simulation
     * @throws NullPointerException if any argument is {@code null}
     */
    public Simulator(Instant startTime) {
        this(startTime, new SimpleActorDurationCalculator());
    }

    /**
     * Constructs a {@link Simulator} object which has customized delaying behaviour and is set to run it's simulation as if from some
     * specific point in time.
     * @param startTime start time of simulation
     * @param actorDurationCalculator determines the delay caused by an actor processing a message
     * @throws NullPointerException if any argument is {@code null}
     */
    public Simulator(Instant startTime, ActorDurationCalculator actorDurationCalculator) {
        Validate.notNull(startTime);
        Validate.notNull(actorDurationCalculator);

        this.events = new PriorityQueue<>();
        this.holders = new HashMap<>();
        this.currentTime = startTime;
        
        Map<Class<? extends Event>, Consumer<Event>> eventHandlers = new HashMap<>();
        eventHandlers.put(CustomEvent.class, this::handleCustomEvent);
        eventHandlers.put(AddActorEvent.class, this::handleAddActorEvent);
        eventHandlers.put(RemoveActorEvent.class, this::handleRemoveActorEvent);
        eventHandlers.put(AddTimerEvent.class, this::handleAddTimerEvent);
        eventHandlers.put(RemoveTimerEvent.class, this::handleRemoveTimerEvent);
        eventHandlers.put(TimerTriggerEvent.class, this::handleTimerTriggerEvent);
        eventHandlers.put(MessageEvent.class, this::handleMessageEvent);
        eventHandlers.put(AddMessageSourceEvent.class, this::handleAddMessageSourceEvent);
        eventHandlers.put(PullFromMessageSourceEvent.class, this::handlePullFromMessageSourceEvent);
        eventHandlers.put(RemoveMessageSourceEvent.class, this::handleRemoveMessageSourceEvent);
        eventHandlers.put(AddMessageSinkEvent.class, this::handleAddMessageSinkEvent);
        eventHandlers.put(RemoveMessageSinkEvent.class, this::handleRemoveMessageSinkEvent);
        
        this.eventHandlers =
                (UnmodifiableMap<Class<? extends Event>, Consumer<Event>>) UnmodifiableMap.unmodifiableMap(eventHandlers);
        this.actorDurationCalculator = actorDurationCalculator;
        
        this.sinks = new HashSet<>();
        this.sources = new HashSet<>();
    }
    
    /**
     * Queue a message sink to be added to this simulation. A {@link MessageSink} can be used to write messages from the simulation to
     * some external source.
     * <p>
     * Note that this method queues an add. As such, this method will returns before operation actually takes place. Any error during
     * encountered during adding will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param sink sink to add
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time
     */
    public void addMessageSink(MessageSink sink, Instant when) {
        Validate.notNull(sink);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");
        
        events.add(new AddMessageSinkEvent(sink, when, nextSequenceNumber++));
    }

    /**
     * Queue a message source to be added to this simulation. A {@link MessageSource} can be used to read messages in to the simulation from
     * some external source.
     * <p>
     * Note that this method queues an add. As such, this method will returns before operation actually takes place. Any error during
     * encountered during adding will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param source source to add
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time
     */
    public void addMessageSource(MessageSource source, Instant when) {
        Validate.notNull(source);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");
        
        events.add(new AddMessageSourceEvent(source, when, nextSequenceNumber++));
    }

    /**
     * Queue a message sink to be removed from this simulation.
     * <p>
     * Note that this method queues a remove. As such, this method will returns before operation actually takes place. Any error during
     * encountered during removing will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param sink sink to remove
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time
     */
    public void removeSink(MessageSink sink, Instant when) {
        Validate.notNull(sink);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");
        
        events.add(new RemoveMessageSinkEvent(sink, when, nextSequenceNumber++));
    }

    /**
     * Queue a message source to be removed from this simulation.
     * <p>
     * Note that this method queues a remove. As such, this method will returns before operation actually takes place. Any error during
     * encountered during removing will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param source source to remove
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time
     */
    public void removeSource(MessageSource source, Instant when) {
        Validate.notNull(source);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");
        
        events.add(new RemoveMessageSourceEvent(source, when, nextSequenceNumber++));
    }

    /**
     * Queue an actor to be added to this simulation. An actor can have a time offset associated with it, meaning that the time passed in to
     * the actor by this simulator can be offset by some amount. The offset is used to simulate actors running on remote systems -- the
     * clocks on two systems are likely to be slightly out of sync, even though they're incrementing at the same rate.
     * <p>
     * Note that this method queues an add. As such, this method will returns before operation actually takes place. Any error during
     * encountered during adding will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param address address of this actor
     * @param actor actor being added
     * @param timeOffset time offset of this actor
     * @param when time in simulation environment when event should take place
     * @param primingMessages messages to pass in to {@code actor} for priming
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time, or {@code timeOffset} is negative
     */
    public void addActor(String address, Coroutine actor, Duration timeOffset, Instant when, Object... primingMessages) {
        Validate.notNull(address);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(when);
        Validate.notNull(primingMessages);
        Validate.noNullElements(primingMessages);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");
        Validate.isTrue(!timeOffset.isNegative(), "Negative time offset not allowed");

        events.add(new AddActorEvent(address, actor, timeOffset, when, nextSequenceNumber++, primingMessages));
    }

    /**
     * Queue an actor to be removed from this simulation.
     * <p>
     * Note that this method queues a remove. As such, this method will returns before operation actually takes place. Any error during
     * encountered during removing will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param address address of actor to remove
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time
     */
    public void removeActor(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");

        events.add(new RemoveActorEvent(address, when, nextSequenceNumber++));
    }

    /**
     * Queue a timer to be added to this simulation. This is used to simulate a {@link TimerGateway}.
     * <p>
     * Note that this method queues an add. As such, this method will returns before operation actually takes place. Any error during
     * encountered during adding will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param address address of this actor
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     */
    public void addTimer(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");

        events.add(new AddTimerEvent(address, when, nextSequenceNumber++));
    }

    /**
     * Queue a timer to be removed from this simulation. Timer events that have not yet been fired are removed along with the timer.
     * <p>
     * Note that this method queues a remove. As such, this method will returns before operation actually takes place. Any error during
     * encountered during removing will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param address address of timer to remove
     * @param when time in simulation environment when event should take place
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code when} is before this simulator's current time
     */
    public void removeTimer(String address, Instant when) {
        Validate.notNull(address);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");

        events.add(new RemoveTimerEvent(address, when, nextSequenceNumber++));
    }

    /**
     * Queue a custom piece of logic to be run by this simulator. Avoid accessing this simulator from the code being run.
     * <p>
     * Note that this method queues an add. As such, this method will returns before operation actually takes place. Any error during
     * encountered during adding will not be known to the caller. Instead, {@link #process() } will encounter an exception when it arrives
     * at the event added by this call.
     * @param runnable runnable to execute
     * @param when time in simulation environment when execution should take place
     */
    public void addCustom(Runnable runnable, Instant when) {
        Validate.notNull(runnable);
        Validate.notNull(when);
        Validate.isTrue(!when.isBefore(currentTime), "Attempting to add event prior to current time");

        events.add(new CustomEvent(runnable, when, nextSequenceNumber++));
    }

    /**
     * Checks to see if this simulation is still running. If this method return {@code true}, it means that events are available for
     * processing. If this method returns {@code false}, it means that no more events are available for processing (the simulation has
     * ended).
     * @return {@code true} if this simulation has another event to process
     */
    public boolean hasMore() {
        return !events.isEmpty();
    }
    
    /**
     * Process the next event. If this simulation has no more events left to process, this method throws an exception. Use
     * {@link #hasMore() } to determine if this method should be called.
     * @return the current time in the simulation
     * @throws IllegalStateException if no more events are left to process
     */
    public Instant process() {
        Event event = events.poll();
        Validate.validState(event != null, "No events left to process");

        currentTime = event.getTriggerTime();
        
        Consumer<Event> eventHandler = eventHandlers.get(event.getClass());
        LOG.debug("Processing event {}", event);
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

    private void handleTimerTriggerEvent(Event event) {
        TimerTriggerEvent timerTriggerEvent = (TimerTriggerEvent) event;        
        queueMessageFromActorOrGateway(
                timerTriggerEvent.getSourceAddress(),
                timerTriggerEvent.getDestinationAddress(),
                timerTriggerEvent.getMessage(),
                Duration.ZERO);
    }
    
    private void handleAddActorEvent(Event event) {
        AddActorEvent joinEvent = (AddActorEvent) event;

        String address = joinEvent.getAddress();
        Coroutine actor = joinEvent.getActor();
        Duration timeOffset = joinEvent.getTimeOffset();
        UnmodifiableList<Object> primingMessages = joinEvent.getPrimingMessages();
        Address addressObj = Address.of(address);
        
        CoroutineRunner actorRunner = new CoroutineRunner(actor);
        
        validateAddressDoesNotConflict(addressObj);

        ActorHolder newHolder = new ActorHolder(addressObj, actorRunner, timeOffset, currentTime);
        holders.put(addressObj, newHolder); // won't overwrite because validateAddresDoesNotConflict above will throw exception if it does

        for (Object message : primingMessages) {
            queueMessageFromActorOrGateway(addressObj, addressObj, message, Duration.ZERO);
        }
    }
    
    private void handleRemoveActorEvent(Event event) {
        RemoveActorEvent removeActorEvent = (RemoveActorEvent) event;

        String address = removeActorEvent.getAddress();
        Address addressObj = Address.of(address);

        Holder holder = holders.remove(addressObj);
        Validate.isTrue(holder != null, "Address does not exist");
        if (!(holder instanceof ActorHolder)) {
            // If the holder removed was not an actor holder, add it back in and throw an exception
            holders.put(addressObj, holder);
            throw new IllegalArgumentException("Address not an actor");
        }
        
        // In addition to removing the actor, we need to remove all messages coming in to the actor. The actor has been removed, which means
        // that it'll never get a chance to receive these messages (we pretend that these messages were already in its receive queue).
        Iterator<Event> eventsIt = events.iterator();
        while (eventsIt.hasNext()) {
            Event pendingEvent = eventsIt.next();
            if (pendingEvent instanceof MessageEvent) {
                MessageEvent messageEvent = (MessageEvent) pendingEvent;
                if (addressObj.isPrefixOf(messageEvent.getDestinationAddress())) {
                    eventsIt.remove();
                }
            }
        }
    }

    private void handleAddTimerEvent(Event event) {
        AddTimerEvent addTimerEvent = (AddTimerEvent) event;

        String address = addTimerEvent.getAddress();
        Address addressObj = Address.of(address);

        validateAddressDoesNotConflict(addressObj);

        TimerHolder newHolder = new TimerHolder(addressObj);
        holders.put(addressObj, newHolder); // won't overwrite because validateAddresDoesNotConflict above will throw exception if it does
    }
    
    private void handleRemoveTimerEvent(Event event) {
        RemoveTimerEvent removeTimerEvent = (RemoveTimerEvent) event;

        String address = removeTimerEvent.getAddress();
        Address addressObj = Address.of(address);

        Holder holder = holders.remove(addressObj);
        Validate.isTrue(holder != null, "Address does not exist");
        if (!(holder instanceof TimerHolder)) {
            // If the holder removed was not a timer holder, add it back in and throw an exception
            holders.put(addressObj, holder);
            throw new IllegalArgumentException("Address not a timer");
        }
        
        // In addition to removing the timer, we need to remove all messages queued by the timer. The timer has been removed, which means
        // that it'll never get a chance to send these messages out.
        Iterator<Event> eventsIt = events.iterator();
        while (eventsIt.hasNext()) {
            Event pendingEvent = eventsIt.next();
            if (pendingEvent instanceof TimerTriggerEvent) {
                TimerTriggerEvent timerTriggerEvent = (TimerTriggerEvent) pendingEvent;
                if (addressObj.isPrefixOf(timerTriggerEvent.getSourceAddress())) {
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
            // The MessageSource was removed, so block this pull event.
            return;
        }
        
        // Read the message from the message source
        SourceMessage sourceMessage;
        try {
            sourceMessage = source.readNextMessage();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read message from source", ex);
        }
        
        if (sourceMessage == null) {
            // We've reached the end of the message source. Remove+close the message source and return.
            try {
                source.close();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to close source", ex);
            }
            sources.remove(source);
            return;
        }
        
        // Queue message
        queueMessageFromMessageSource(sourceMessage.getSource(), sourceMessage.getDestination(), sourceMessage.getMessage());
        
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
    
    private void validateAddressDoesNotConflict(Address address) {
        Validate.notNull(address);
        Validate.isTrue(!address.isEmpty());
        Holder conflictingHolder = findHolder(address);
        Validate.isTrue(conflictingHolder == null,
                "Address {} conflicts with existing address {}", address, conflictingHolder);
    }

    private Holder findHolder(Address address) {
        Validate.notNull(address);
        Validate.isTrue(!address.isEmpty());

        List<String> splitAddress = address.getElements();
        
        for (int i = address.size(); i >= 1; i--) {
            Address testAddress = Address.of(splitAddress.subList(0, i));
            Holder actorHolder = holders.get(testAddress);
            if (actorHolder != null) {
                return actorHolder;
            }
        }
        
        return null;
    }
    
    private void queueMessageFromMessageSource(
            Address sourceAddress,
            Address destinationAddress,
            Object message) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.isTrue(!sourceAddress.isEmpty());
        Validate.isTrue(!destinationAddress.isEmpty());
        
        Instant arriveTime = currentTime;
        
        // Messages generated from message sources don't require any durations to be added to the message or the actual message's source to
        // exist.
        
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

    private void queueMessageFromActorOrGateway(
            Address sourceAddress,
            Address destinationAddress,
            Object message,
            Duration actorExecDuration) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notNull(actorExecDuration);
        Validate.isTrue(!actorExecDuration.isNegative());
        
        // Source must exist.
        //
        // Destination doesn't have to exist. It's perfectly valid to send a message to an actor that doesn't exist yet but may have come in
        // to existance exist by the time the message arrives.
        Validate.isTrue(findHolder(sourceAddress) != null);
        
        Instant arriveTime = currentTime;
        
        // Add how far in the future this message is sent. This is here because an actors's onStep() may take some time to execute. Messages
        // sent by that actor are sent after the onStep() completes. As such, we need to make it seem as if this message was sent in the
        // future after onStep()'s completion.
        //
        // If not sent by an actor, this value should be Duration.ZERO
        arriveTime = arriveTime.plus(actorExecDuration);

        
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
    
    private void queueTimerTrigger(
            boolean sourceMustExistFlag,
            Address sourceAddress,
            Address destinationAddress,
            Object message,
            Duration scheduledDuration) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        Validate.notNull(scheduledDuration);
        Validate.isTrue(!scheduledDuration.isNegative());
        
        // Source must exist.
        //
        // Destination doesn't have to exist. It's perfectly valid to send a message to an actor that doesn't exist yet but may have come in
        // to existance exist by the time the message arrives.
        if (sourceMustExistFlag) {
            // Only check to see if source actor exists if we the flag is set to do so
            Validate.isTrue(findHolder(sourceAddress) != null);
        }
        
        Instant arriveTime = currentTime;
        
        // Message may be scheduled by the timer to run at a certain delay. If it is, add that scheduled delay to the arrival time.
        arriveTime = arriveTime.plus(scheduledDuration);
        
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
        
        events.add(new TimerTriggerEvent(sourceAddress, destinationAddress, message, arriveTime, nextSequenceNumber++));
    }
    
    private void processMessage(MessageEvent messageEvent) {
        Validate.notNull(messageEvent);
        
        Address source = messageEvent.getSourceAddress();
        Address destination = messageEvent.getDestinationAddress();
        Object message = messageEvent.getMessage();
        
        // Get destination
        Holder holder = findHolder(destination);

        // If destination doesn't exist, log a warning and move on. In the real world if the destination doesn't exist or is otherwise
        // unreachable, the same thing happens -- message is sent to some destination but the system doesn't really care if it arrives or
        // not.
        if (holder == null) {
            LOG.warn("Processing message to destination that doesn't exist: {}", destination);
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

    private void processMessageToTimer(TimerHolder destHolder, Address destination, Address source, Object message) {
        // If this is a timer messages, bounce it back to the source -- scheduled to arrive after the specified duration
        Duration timerDuration;
        try {
            String durationStr = destination.getElement(1);
            timerDuration = Duration.ofMillis(Long.parseLong(durationStr));
            Validate.isTrue(!timerDuration.isNegative());
        } catch (Exception e) {
            LOG.warn("Processing message to destination that doesn't exist: {}", destination);
            // TODO log here. nothing else, technically if the timer gets an unparsable duration it'll block the message
            return;
        }
        queueTimerTrigger(true, destination, source, message, timerDuration);
    }

    private void processMessageToActor(ActorHolder destHolder, Address destination, Address source, Object message) {
        // Earliest possible step time is the minimum point in time which the destination actor can have its onStep called again. That is,
        // the message being processed must be >= to the earliest possible step time. Otherwise, something has gone wrong. This is a sanity
        // check.
        Instant minTime = destHolder.getEarliestPossibleOnStepTime();
        Validate.isTrue(!currentTime.isBefore(minTime));


        // Set up values for calling onStep, and then call onStep. If onStep throws an exception, then log and remove the actor from the
        // test harness. In the real world, if an actor throws an exception, it'll get removed from the list of actors assigned to that
        // ActorRunner/ActorRunnable and no more execution is done on it.
        CoroutineRunner actorRunner = destHolder.getActorRunner();
        Address address = destHolder.getAddress();
        SourceContext context = destHolder.getContext();
        Instant localActorTime = currentTime.plus(destHolder.getTimeOffset()); // This is the time as it appears to the actor. Clocks
                                                                               // between different machines are never going to be entirely
                                                                               // in sync. So, one actor may technically have a different
                                                                               // local time than another (because they may be running on
                                                                               // different machines).

        for (MessageSink sink : sinks) {
            try {
                sink.writeNextMessage(source, destination, localActorTime, message);
            } catch (IOException ioe) {
                throw new IllegalStateException("Unable to write to sink", ioe);
            }
        }

        Duration realExecDuration;
        boolean stopped;
        Instant execStartTime = Instant.now();
        try {
            stopped = SourceContext.fire(context, source, destination, localActorTime, message);
        } catch (Exception e) {
            LOG.warn("Actor " + destHolder.getAddress() + " threw an exception", e);
            stopped = true;
        }
        Instant execEndTime = Instant.now();
        realExecDuration = Duration.between(execStartTime, execEndTime);

        // We've finished calling onStep(). Next, add the amount of time it took to do the processing of the message by onStep. This is a
        // calculated value. We have the real execution time, and we pass that in as a hint to the interface that does the calculations, but
        // ultimately the interface can specify whatever duration it wants (provided that it isn't negative).
        //
        // Add the execution duration to the current time and set it as the actor's next possible step time. There's no way we can call
        // onStep() before this time. Otherwise we'd be going back in time.
        if (realExecDuration.isNegative()) { // System clock may be wonky, so make sure exec duration doesn't come out as negative!
            realExecDuration = Duration.ZERO;
        }
        Duration execDuration = actorDurationCalculator.calculateDuration(source, destination, message, realExecDuration);
        Validate.isTrue(!execDuration.isNegative()); // sanity check here, make sure calculated duration it isn't negative

        Instant earliestPossibleOnStepTime = currentTime.plus(execDuration);
        destHolder.setEarliestPossibleOnStepTime(earliestPossibleOnStepTime);
        
        // All messages going out from this onStep() call go out at the end, which means that their arrival time needs to have execDuration
        // added to it as well.
        List<BatchedOutgoingMessage> batchedOutMsgs = context.copyAndClearOutgoingMessages();
        for (BatchedOutgoingMessage batchedOutMsg : batchedOutMsgs) {
            queueMessageFromActorOrGateway(
                    batchedOutMsg.getSource(),
                    batchedOutMsg.getDestination(),
                    batchedOutMsg.getMessage(),
                    execDuration);
        }

        // Go through any messages queued to arrive at this actor before earliest possible onstep time. Reschedule each of those messages
        // such that they arrive at earliest possible on step time. Like the comment above says, it wouldn't make sense to call onStep()
        // before this time. We'd be going back in time if we did.
        List<MessageEvent> messageEventsToReschedule = new LinkedList<>();
        Iterator<Event> it = events.iterator();
        while (it.hasNext()) {
            Event event = it.next();
            
//          THE BLOCK BELOW HAS BEEN COMMENTED OUT BECAUSE IT IS INCORRECT. PriorityQueue.iterator() DOES NOT RETURN ELEMENTS IN ORDER. THIS
//          IS A REMINDER NOT TO PUT IT BACK IN!!!! ANOTHER IF BLOCK WAS ADDED BELOW THAT WILL SKIP THE ENTRY VIA continue; RATHER THAN
//          STOP THE LOOP VIA break;
//            // Events queue is ordered. If the event we're looking at right now is >= to the earliest possible onstep time, it means that
//            // this event and all events after it don't need to be rescheduled, so stop checking here.
//            if (!earliestPossibleOnStepTime.isAfter(event.getTriggerTime())) {
//                break;
//            }

            // If the event we're looking at right now is >= to the earliest possible onstep time, it means that this event doesn't need to
            // be rescheduled, so skip this event and go to the next one.
            if (!earliestPossibleOnStepTime.isAfter(event.getTriggerTime())) {
                continue;
            }

            // We only care about MessageEvents because they're the ones that trigger calls to onStep(). If the event is a MessageEvent and
            // the destination of that event is this actor, remove it from the events queue and add it to a temporary collection. All items
            // in this temporary collection are going to be rescheduled such that they trigger at earliestPossibleOnStepTime and re-added
            // to the events queue.
            if (event instanceof MessageEvent) {
                MessageEvent pendingMessageEvent = (MessageEvent) event;
                if (pendingMessageEvent.getDestinationAddress().equals(address)) {
                    MessageEvent updatedMessageEvent = new MessageEvent(
                            pendingMessageEvent.getSourceAddress(),
                            pendingMessageEvent.getDestinationAddress(),
                            pendingMessageEvent.getMessage(),
                            earliestPossibleOnStepTime,
                            nextSequenceNumber++);
                    messageEventsToReschedule.add(updatedMessageEvent);
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
        
        
        if (stopped) {
            // Actor stopped or crashed. Remove it from the list of actors and stop processing. We do this here at the end of hte method
            // because the actor may have sent stuff out before it termianted. We want the stuff that was sent out to make it to its
            // destination (logic to send it above this if block, and checks to crashes if holder.contains(address) == false).
            holders.remove(address);
        }
    }
}
