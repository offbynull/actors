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
package com.offbynull.peernetic.rpc.transport.internal;

import org.apache.commons.lang3.Validate;

/**
 * Command passed in to a transport to send a message.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class SendMessageCommand<A> {
    private Object content;
    private A destination;

    /**
     * Construct a {@link SendMessageCommand} object.
     * @param content message content
     * @param destination message destination
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SendMessageCommand(Object content, A destination) {
        Validate.notNull(content);
        Validate.notNull(destination);

        this.content = content;
        this.destination = destination;
    }

    /**
     * Get the message content.
     * @return message content
     */
    public Object getContent() {
        return content;
    }

    /**
     * Get the message destination.
     * @return message destination
     * @return 
     */
    public A getDestination() {
        return destination;
    }
    
}
