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
package com.offbynull.actors.core.gateways.direct;

import static com.offbynull.actors.core.gateway.CommonAddresses.DEFAULT_DIRECT;
import com.offbynull.actors.core.gateway.Gateway;
import com.offbynull.actors.core.shuttle.Shuttle;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import com.offbynull.actors.core.shuttles.simple.Bus;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that allows you read and write messages from normal Java code.
 * <p>
 * To read messages coming into a specific address, first register it for listening by calling 
 * {@link #listen(com.offbynull.actors.core.shuttle.Address) } then read messages by calling
 * {@link #readMessage(com.offbynull.actors.core.shuttle.Address, long, java.util.concurrent.TimeUnit) }.
 * <p>
 * To write messages from a specific address, call {@link #writeMessage(com.offbynull.actors.core.shuttle.Message) }.
 * @author Kasra Faghihi
 */
public final class DirectGateway implements Gateway {


    private final String prefix;
    private final ConcurrentHashMap<Address, Bus> readQueues;
    private final ConcurrentHashMap<String, Shuttle> outShuttles;
    private final DirectShuttle shuttle;
    
    private final CountDownLatch joinerLatch;

    /**
     * Create a {@link DirectGateway} instance. Equivalent to calling {@code create(DefaultAddresses.DEFAULT_DIRECT)}.
     * @return new direct gateway
     */
    public static DirectGateway create() {
        return create(DEFAULT_DIRECT);
    }

    /**
     * Create a {@link DirectGateway} instance.
     * @param prefix address prefix for this gateway
     * @return new direct gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public static DirectGateway create(String prefix) {
        DirectGateway gateway = new DirectGateway(prefix);
        return gateway;
    }

    private DirectGateway(String prefix) {
        Validate.notNull(prefix);
        
        this.prefix = prefix;
        this.readQueues = new ConcurrentHashMap<>();
        this.outShuttles = new ConcurrentHashMap<>();
        this.shuttle = new DirectShuttle(prefix, readQueues);
        
        this.joinerLatch = new CountDownLatch(1);
    }

    @Override
    public Shuttle getIncomingShuttle() {
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        String shuttlePrefix = shuttle.getPrefix();
        outShuttles.put(shuttlePrefix, shuttle);
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        outShuttles.remove(shuttlePrefix);
    }

    /**
     * Registers an address for listening. The input address and all addresses under it will be listenable.
     * @param listenAddress address to register
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} has already been registered, or if {@code listenAddress} does not begin
     * with the prefix for this gateway
     * @throws IllegalStateException if this gateway is closed
     */
    public void listen(Address listenAddress) {
        Validate.notNull(listenAddress);
        Validate.isTrue(listenAddress.getElement(0).equals(prefix));
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        Bus existingBus = readQueues.putIfAbsent(listenAddress, new Bus());
        Validate.isTrue(existingBus == null, "Listener already registered");
    }
    
    /**
     * Equivalent to calling {@code listen(Address.fromString(address))}.
     * @param listenAddress address to unregister
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} has already been registered, or if {@code address} does not begin with the
     * prefix for this gateway
     * @throws IllegalStateException if this gateway is closed
     */
    public void listen(String listenAddress) {
        Validate.notNull(listenAddress);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        listen(Address.fromString(listenAddress));
    }

    /**
     * Unregisters an address for listening. If the address was never registered, nothing happens.
     * @param listenAddress address to unregister
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} does not begin with the prefix for this gateway
     * @throws IllegalStateException if this gateway is closed
     */
    public void unlisten(Address listenAddress) {
        Validate.notNull(listenAddress);
        Validate.isTrue(listenAddress.getElement(0).equals(prefix));
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        readQueues.remove(listenAddress);
    }

    /**
     * Equivalent to calling {@code unlisten(Address.fromString(address))}.
     * @param listenAddress address to unregister
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} does not begin with the prefix for this gateway
     * @throws IllegalStateException if this gateway is closed
     */
    public void unlisten(String listenAddress) {
        Validate.notNull(listenAddress);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        unlisten(Address.fromString(listenAddress));
    }

    /**
     * Write a message to another gateway.
     * @param message outoging message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any argument is {@code message.getSourceAddress()} doesn't start with this gateway's prefix
     * @throws IllegalStateException if this gateway is closed
     */
    public void writeMessage(Message message) {
        Validate.notNull(message);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        String srcPrefix = message.getSourceAddress().getElement(0);
        String dstPrefix = message.getDestinationAddress().getElement(0);

        Validate.isTrue(prefix.equals(srcPrefix));

        Shuttle dstShuttle = outShuttles.get(dstPrefix);
        if (dstShuttle != null) {
            dstShuttle.send(message);
        }
    }

    /**
     * Equivalent to calling {@code writeMessage(new Message(source, destination, message))}.
     * @param source source address
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} does not start with this gateway's prefix
     * @throws IllegalStateException if this gateway is closed
     */
    public void writeMessage(Address source, Address destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        writeMessage(new Message(source, destination, message));
    }

    /**
     * Equivalent to calling {@code writeMessage(Address.fromString(source), Address.fromString(destination), message))}.
     * @param source source address
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code source} does not start with this gateway's prefix
     * @throws IllegalStateException if this gateway is closed
     */
    public void writeMessage(String source, String destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        writeMessage(Address.fromString(source), Address.fromString(destination), message);
    }

    /**
     * Equivalent to calling {@code writeMessage(new Message(Address.of(getIncomingShuttle().getPrefix()), destination, message))}.
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if this gateway is closed
     */
    public void writeMessage(Address destination, Object message) {
        Validate.notNull(destination);
        Validate.notNull(message);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        writeMessage(new Message(Address.of(prefix), destination, message));
    }
    
    /**
     * Equivalent to calling {@code writeMessage(Address.fromString(destination), message))}.
     * @param destination destination address
     * @param message message to send
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if this gateway is closed
     */
    public void writeMessage(String destination, Object message) {
        Validate.notNull(destination);
        Validate.notNull(message);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        writeMessage(Address.fromString(destination), message);
    }

    /**
     * Reads a message sent to this gateway.
     * @param listenAddress address registered for listening
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming message payload, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening
     * @throws IllegalStateException if this gateway is closed
     * @throws InterruptedException if this thread is interrupted
     */
    public Message readMessage(Address listenAddress, long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(listenAddress);
        Validate.notNull(unit);
        Validate.isTrue(timeout >= 0L);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        Address next = listenAddress;
        do {
            Bus queue = readQueues.get(next);
            if (queue != null) {
                List<Object> messageList = queue.pull(1, timeout, unit);
                if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
                    throw new IllegalStateException();
                }

                return messageList.isEmpty() ? null : (Message) messageList.get(0);
            }
            next = next.removeSuffix(1);
        } while (next.size() >= 1);

        throw new IllegalArgumentException(listenAddress + " not registered for listening");
    }

    /**
     * Equivalent to calling {@code readMessage(Address.fromString(listenAddress), timeout, unit)}.
     * @param listenAddress address registered for listening
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming message payload, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening
     * @throws IllegalStateException if this gateway is closed
     * @throws InterruptedException if this thread is interrupted
     */
    public Message readMessage(String listenAddress, long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(listenAddress);
        Validate.notNull(unit);
        Validate.isTrue(timeout >= 0L);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        return readMessage(Address.fromString(listenAddress), timeout, unit);
    }

    /**
     * Equivalent to calling {@code readMessage(listenAddress, 0L, TimeUnit.MILLISECONDS)}.
     * @param listenAddress address registered for listening
     * @return incoming message payload, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening
     * @throws IllegalStateException if this gateway is closed
     * @throws InterruptedException if this thread is interrupted
     */
    public Message readMessage(Address listenAddress) throws InterruptedException {
        Validate.notNull(listenAddress);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        return readMessage(listenAddress, 0L, TimeUnit.MILLISECONDS);
    }

    /**
     * Equivalent to calling {@code readMessage(Address.fromString(listenAddress))}.
     * @param listenAddress address registered for listening
     * @return incoming message payload, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening
     * @throws IllegalStateException if this gateway is closed
     * @throws InterruptedException if this thread is interrupted
     */
    public Message readMessage(String listenAddress) throws InterruptedException {
        Validate.notNull(listenAddress);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }

        return readMessage(Address.fromString(listenAddress));
    }

    /**
     * Equivalent to calling {@code readMessage(listenAddress, timeout, unit).getMessage()} but with a {@code null} check.
     * @param <T> expected payload type
     * @param listenAddress address registered for listening
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming message payload only, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening, or if {@code timeout < 0}
     * @throws IllegalStateException if this gateway is closed
     * @throws ClassCastException if the returned message was not of the expected type
     * @throws InterruptedException if this thread is interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> T readMessagePayloadOnly(Address listenAddress, long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(listenAddress);
        Validate.notNull(unit);
        Validate.isTrue(timeout >= 0L);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        Message msg = readMessage(listenAddress, timeout, unit);
        return msg == null ? null : (T) msg.getMessage();
    }

    /**
     * Equivalent to calling {@code readMessage(Address.fromString(listenAddress), timeout, unit).getMessage()} but with a {@code null}
     * check.
     * @param <T> expected payload type
     * @param listenAddress address registered for listening
     * @param timeout how long to wait before giving up, in units of {@code unit} unit
     * @param unit a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return incoming message payload only, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening, or if {@code timeout < 0}
     * @throws IllegalStateException if this gateway is closed
     * @throws ClassCastException if the returned message was not of the expected type
     * @throws InterruptedException if this thread is interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> T readMessagePayloadOnly(String listenAddress, long timeout, TimeUnit unit) throws InterruptedException {
        Validate.notNull(listenAddress);
        Validate.notNull(unit);
        Validate.isTrue(timeout >= 0L);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        Message msg = readMessage(Address.fromString(listenAddress), timeout, unit);
        return msg == null ? null : (T) msg.getMessage();
    }

    /**
     * Equivalent to calling {@code readMessagePayloadOnly(listenAddress, 0L, TimeUnit.MILLISECONDS)}.
     * @param <T> expected payload type
     * @param listenAddress address registered for listening
     * @return incoming message payload only, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening
     * @throws IllegalStateException if this gateway is closed
     * @throws ClassCastException if the returned message was not of the expected type
     * @throws InterruptedException if this thread is interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> T readMessagePayloadOnly(Address listenAddress) throws InterruptedException {
        Validate.notNull(listenAddress);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        return readMessagePayloadOnly(listenAddress, 0L, TimeUnit.MILLISECONDS);
    }

    /**
     * Equivalent to calling {@code readMessagePayloadOnly(Address.fromString(listenAddress))}.
     * @param <T> expected payload type
     * @param listenAddress address registered for listening
     * @return incoming message payload only, or {@code null} if no message came in before the timeout
     * @throws NullPointerException if any argument is {@code null} 
     * @throws IllegalArgumentException if {@code listenAddress} was not registered for listening
     * @throws IllegalStateException if this gateway is closed
     * @throws ClassCastException if the returned message was not of the expected type
     * @throws InterruptedException if this thread is interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> T readMessagePayloadOnly(String listenAddress) throws InterruptedException {
        Validate.notNull(listenAddress);
        if (joinerLatch.getCount() == 0L) { // latch will be at 0 when closed
            throw new IllegalStateException();
        }
        
        return readMessagePayloadOnly(Address.fromString(listenAddress));
    }
    
    @Override
    public void close() {
        readQueues.clear();
        outShuttles.clear();
        joinerLatch.countDown();
    }

    @Override
    public void join() throws InterruptedException {
        joinerLatch.await();
    }
}
