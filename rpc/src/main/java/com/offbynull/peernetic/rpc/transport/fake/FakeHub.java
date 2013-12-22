/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.rpc.transport.fake;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * A hub that pipes messages between {@link FakeTransport}s.
 * @author Kasra F
 * @param <A> address type
 */
public final class FakeHub<A> {

    private Line<A> line;
    

    private EventLoop eventLoop;
    private LinkedBlockingQueue<Command> commandQueue;
    
    private volatile State state;
    private Lock startStopLock;

    /**
     * Construct a {@link FakeHub} object.
     * @param line line to use
     * @throws NullPointerException if any arguments are {@code null}
     */
    public FakeHub(Line<A> line) {
        Validate.notNull(line);

        this.line = line;

        eventLoop = new EventLoop();
        commandQueue = new LinkedBlockingQueue<>();
        
        state = State.UNKNOWN;
        startStopLock = new ReentrantLock();
    }

    /**
     * Start.
     * @throws IOException on error
     * @throws IllegalStateException if already started
     */
    public void start() throws IOException {
        startStopLock.lock();
        try {
            Validate.validState(state == State.UNKNOWN);
            
            eventLoop.startAndWait();
            state = State.STARTED;
        } finally {
            startStopLock.unlock();
        }
    }

    /**
     * Stop.
     * @throws IllegalStateException if not started
     */
    public void stop() {
        startStopLock.lock();
        try {
            Validate.validState(state == State.STARTED);
            
            eventLoop.stopAndWait();
            state = State.STOPPED;
        } finally {
            startStopLock.unlock();
        }
    }

    /**
     * Queues a command to add an endpoint to the hub.
     * @param address endpoint address
     * @param receiver receiver that receives messages from the hub to {@code address}
     * @return an object to send messages to the hub from {@code address}
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if not started
     */
    FakeHubSender<A> addEndpoint(A address, FakeHubReceiver<A> receiver) {
        Validate.notNull(address);
        Validate.notNull(receiver);

        Validate.validState(state == State.STARTED);
        
        CommandAddEndpoint<A> command = new CommandAddEndpoint<>(address, receiver);
        commandQueue.add(command);
        
        return new FakeHubSender<>(commandQueue, line);
    }

    /**
     * Queues a command to remove an endpoint from the hub.
     * @param address endpoint address
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if not started
     */
    void removeEndpoint(A address) {
        Validate.notNull(address);

        Validate.validState(state == State.STARTED);
        
        CommandRemoveEndpoint<A> command = new CommandRemoveEndpoint<>(address);
        commandQueue.add(command);
    }

    /**
     * Queues a command to activates an endpoint from the hub. Call this after
     * {@link #addEndpoint(java.lang.Object, com.offbynull.rpc.transport.fake.FakeHubReceiver) }.
     * @param address endpoint address
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if not started
     * @throws IllegalArgumentException if {@code address} does not exist as an endpoint on the hub
     */
    void activateEndpoint(A address) {
        Validate.notNull(address);

        Validate.validState(state == State.STARTED);
        
        CommandActivateEndpoint<A> command = new CommandActivateEndpoint<>(address);
        commandQueue.add(command);
    }

    private final class EventLoop extends AbstractExecutionThreadService {

        private volatile boolean stop;
        private PriorityQueue<Message<A>> transitMessageQueue;
        private Map<A, FakeEndpoint<A>> addressMap;

        public EventLoop() {
            transitMessageQueue = new PriorityQueue<>(11, new MessageArriveTimeComparator());
            addressMap = new HashMap<>();
        }


        @Override
        public void run() throws Exception {
            while (true) {
                Message<A> nextArrivingMessage = transitMessageQueue.peek();
                
                long waitTime = Long.MAX_VALUE;
                if (nextArrivingMessage != null) {
                    long currentTime = System.currentTimeMillis();
                    long arrivalTime = nextArrivingMessage.getArriveTime();
                    waitTime = Math.max(0L, arrivalTime - currentTime); // just incase
                }
                    
                LinkedList<Command> commands = new LinkedList<>();
                Command initialCommand = commandQueue.poll(waitTime, TimeUnit.MILLISECONDS);
                if (initialCommand != null) {
                    commands.add(initialCommand);
                    commandQueue.drainTo(commands);
                }
                
                long time = System.currentTimeMillis();
                
                // if stop triggered
                if (stop) {
                    break;
                }
                
                // process commands
                for (Command command : commands) {
                    if (command instanceof CommandAddEndpoint) {
                        CommandAddEndpoint<A> commandAe = (CommandAddEndpoint<A>) command;
                        FakeEndpoint<A> endpoint = new FakeEndpoint<>(commandAe.getReceiver());
                        addressMap.put(commandAe.getAddress(), endpoint);
                    } else if (command instanceof CommandRemoveEndpoint) {
                        CommandRemoveEndpoint<A> commandRe = (CommandRemoveEndpoint<A>) command;
                        addressMap.remove(commandRe.getAddress());
                    } else if (command instanceof CommandActivateEndpoint) {
                        CommandActivateEndpoint<A> commandAe = (CommandActivateEndpoint<A>) command;
                        FakeEndpoint<A> endpoint = addressMap.get(commandAe.getAddress());
                        endpoint.setActive(true);
                    } else if (command instanceof CommandSend) {
                        CommandSend<A> commandSend = (CommandSend<A>) command;
                        Message<A> message = commandSend.getMessage();
                        transitMessageQueue.add(message);
                    } else {
                        throw new IllegalStateException();
                    }
                }

                // process arrived messages
                List<Message<A>> packets = new LinkedList<>();

                while (!transitMessageQueue.isEmpty()) {
                    Message<A> topPacket = transitMessageQueue.peek();

                    long arriveTime = topPacket.getArriveTime();
                    if (arriveTime > time) {
                        break;
                    }
                    
                    transitMessageQueue.poll(); // remove
                    
                    FakeEndpoint<A> dest = addressMap.get(topPacket.getTo());
                    if (dest == null || !dest.isActive()) {
                        continue;
                    }

                    try {
                        dest.getReceiver().incoming(topPacket);
                    } catch (RuntimeException re) { // NOPMD
                        // do nothing
                    }
                }
                
                line.arrive(packets);
            }
        }

        @Override
        public void triggerShutdown() {
            stop = true;
            commandQueue.add(new Command() { }); // add no-op just to trigger a poll
        }
    }
    
    private enum State {
        UNKNOWN,
        STARTED,
        STOPPED
    }
}
