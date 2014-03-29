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
package com.offbynull.peernetic.actor;

/**
 * Utility class for dealing with messages.
 * @author Kasra Faghihi
 */
public final class MessageUtils {
    private MessageUtils() {
    }
    
    /**
     * Convert an {@link Outgoing} message to a {@link Incoming} message.
     * @param source source for incoming message
     * @param outgoing outgoing message to convert to an incoming message
     * @return incoming message
     * @throws IllegalArgumentException if outgoing message type is not recognized
     */
    public static Incoming flip(Endpoint source, Outgoing outgoing) {
        return new Incoming(outgoing.getContent(), source);
    }
}
