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
package com.offbynull.peernetic.core.gateways.direct;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.gateway.OutputGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that allows you read and write messages using normal Java code.
 * <p>
 * In the following example, the {@link Actor} called {@code echoer} gets a message from {@link DirectGateway} and echoes it back.
 * <pre>
 * Coroutine echoer = (cnt) -&gt; {
 *     Context ctx = (Context) cnt.getContext();
 * 
 *     Address sender = ctx.getSource();
 *     Object msg = ctx.getIncomingMessage();
 *     ctx.addOutgoingMessage(sender, msg);
 * };
 * 
 * ActorRunner actorRunner = new ActorRunner("actors");
 * DirectGateway directGateway = new DirectGateway("direct");
 * 
 * directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
 * actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
 * 
 * actorRunner.addCoroutineActor("echoer", echoerActor);
 * Address echoerAddress = Address.of("actors", "echoer");
 * 
 * String expected;
 * String actual;
 * 
 * directGateway.writeMessage(echoerAddress, "echotest");
 * response = (String) directGateway.readMessages().get(0).getMessage();
 * System.out.println(response);
 * 
 * actorRunner.close();
 * directGateway.close();
 * </pre>
 * @author Kasra Faghihi
 */
public final class DirectGateway implements InputGateway, OutputGateway {

    private final Thread thread;
    private final Bus bus;
    private final LinkedBlockingQueue<Message> readQueue;
    
    private final SimpleShuttle shuttle;

    /**
     * Constructs a {@link DirectGateway} instance.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if failed to initialize the underlying {@link SecureRandom} object used for message ids
     */
    public DirectGateway(String prefix) {
        Validate.notNull(prefix);
        
        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        readQueue = new LinkedBlockingQueue<>();
        thread = new Thread(new DirectRunnable(bus, readQueue));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
        thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        bus.add(new AddShuttle(shuttle));
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        bus.add(new RemoveShuttle(shuttlePrefix));
    }

    /**
     * Writes one message to an actor or gateway. Equivalent to calling
     * {@code writeMessages(new Message(Address.of(getIncomingShuttle().getPrefix()), destination, message))}.
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any source address in {@code messages} does not start with this gateway's prefix
     */
    public void writeMessage(Address destination, Object message) {
        Validate.notNull(destination);
        Validate.notNull(message);
        
        String prefix = shuttle.getPrefix();
        
        writeMessages(new Message(Address.of(prefix), destination, message));
    }
    
    /**
     * Writes one or more messages to an actor or gateway.
     * @param messages messages to send
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     * @throws IllegalArgumentException if any source address in {@code messages} does not start with this gateway's prefix
     */
    public void writeMessages(Message ... messages) {
        Validate.notNull(messages);
        
        String prefix = shuttle.getPrefix();
        List<Message> messageList = new ArrayList<>(messages.length);
        for (Message message : messages) {
            Validate.notNull(message); // explicitly check for nullness, although next line should do this as well
            Validate.isTrue(message.getSourceAddress().getElement(0).equals(prefix));
            messageList.add(message);
        }
        
        
        bus.add(new SendMessages(messageList));
    }

    /**
     * Reads one or more messages sent to this gateway.
     * @return incoming messages
     * @throws InterruptedException if this thread is interrupted
     */
    public List<Message> readMessages() throws InterruptedException {
        List<Message> ret = new LinkedList<>();
        
        Message msg = readQueue.take();
        ret.add(msg);
        readQueue.drainTo(ret);
        
        return ret;
    }

    /**
     * Reads one or more messages sent to this gateway.
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming messages
     * @throws NullPointerException if any argument is {@code null}
     * @throws InterruptedException if this thread is interrupted
     */
    public List<Message> readMessages(long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(unit);
        
        List<Message> ret = new LinkedList<>();
        
        Message msg = readQueue.poll(timeout, unit);
        ret.add(msg);
        readQueue.drainTo(ret);
        
        return ret;
    }
    
    @Override
    public void close() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
}
