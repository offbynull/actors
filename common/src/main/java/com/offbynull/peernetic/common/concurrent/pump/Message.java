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
package com.offbynull.peernetic.common.concurrent.pump;

import org.apache.commons.lang3.Validate;

/**
 * Encapsulates a message.
 * @author Kasra Faghihi
 */
public final class Message {
    private PumpWriter responseWriter;
    private Object message;

    /**
     * Constructs a {@link Message} object.
     * @param responseWriter writer that takes in responses for this message -- can be {@code null} if this message doesn't expect a
     * response
     * @param message contents of message
     * @throws NullPointerException if {@code message} is {@code null}
     */
    public Message(PumpWriter responseWriter, Object message) {
        Validate.notNull(message);
        this.responseWriter = responseWriter;
        this.message = message;
    }

    /**
     * Gets the writer to feed responses for this message to. Can be {@code null}
     * @return writer to feed responses for this message to, or {@code null} if this message doesn't expect a response
     */
    public PumpWriter getResponseWriter() {
        return responseWriter;
    }

    /**
     * Gets the contents of this message.
     * @return contents of this message
     */
    public Object getMessage() {
        return message;
    }
}
