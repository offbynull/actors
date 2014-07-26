/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.debug.testnetwork.messages;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * Transit message.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class TransitMessage<A> {
    private A source;
    private A destination;
    private ByteBuffer data;
    private Instant departTime;
    private Duration duration;

    /**
     * Constructs a {@link TransitMessage} object.
     * @param source source
     * @param destination destination
     * @param data contents
     * @param departTime time packet departed
     * @param duration duration the packet should stay in transit (before arriving)
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code duration} is negative
     */
    public TransitMessage(A source, A destination, ByteBuffer data, Instant departTime, Duration duration) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(data);
        Validate.notNull(departTime);
        Validate.notNull(duration);
        Validate.isTrue(!duration.isNegative());
        this.source = source;
        this.destination = destination;
        this.data = ByteBuffer.allocate(data.remaining());
        this.data.put(data);
        this.data.flip();
        this.departTime = departTime;
        this.duration = duration;
    }

    /**
     * Get source address.
     * @return source
     */
    public A getSource() {
        return source;
    }

    /**
     * Get destination address.
     * @return destination
     */
    public A getDestination() {
        return destination;
    }

    /**
     * Get contents as read-only buffer.
     * @return contents
     */
    public ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    /**
     * Get time which transit of this message began.
     * @return time which transit began
     */
    public Instant getDepartTime() {
        return departTime;
    }

    /**
     * Get duration which the packet should stay in transit.
     * @return duration which the package should stay in transit
     */
    public Duration getDuration() {
        return duration;
    }
    
}