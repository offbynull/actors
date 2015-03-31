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
package com.offbynull.peernetic.core.actors.unreliableproxy;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class TransitMessage {
    private String source;
    private String destination;
    private ByteBuffer data;
    private Instant departTime;
    private Duration duration;

    TransitMessage(String source, String destination, ByteBuffer data, Instant departTime, Duration duration) {
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

    String getSource() {
        return source;
    }

    String getDestination() {
        return destination;
    }

    ByteBuffer getData() {
        return data.asReadOnlyBuffer();
    }

    Instant getDepartTime() {
        return departTime;
    }

    Duration getDuration() {
        return duration;
    }
}