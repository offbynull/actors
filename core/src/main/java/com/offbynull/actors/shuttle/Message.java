/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.shuttle;

import com.offbynull.actors.address.Address;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * A message.
 * @author Kasra Faghihi
 */
public final class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final Address sourceAddress;
    private final Address destinationAddress;
    private final Object message;

    /**
     * Constructs a {@link Message} instance.
     * @param sourceAddress source address of this message
     * @param destinationAddress destination address of this message
     * @param message content of this message
     * @throws NullPointerException if any argument is {@code null}
     */
    public Message(Address sourceAddress, Address destinationAddress, Object message) {
        Validate.notNull(sourceAddress);
        Validate.notNull(destinationAddress);
        Validate.notNull(message);
        
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.message = message;
    }

    /**
     * Constructs a {@link Message} instance. Equivalent to calling
     * {@code new Message(Address.fromString(sourceAddress), Address.fromString(destinationAddress), message)}
     * @param sourceAddress source address of this message
     * @param destinationAddress destination address of this message
     * @param message content of this message
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if either {@code sourceAddress} or {@code destinationAddress} is empty
     */
    public Message(String sourceAddress, String destinationAddress, Object message) {
        this(Address.fromString(sourceAddress), Address.fromString(destinationAddress), message);
    }

    /**
     * Get the source address.
     * @return source address
     */
    public Address getSourceAddress() {
        return sourceAddress;
    }

    /**
     * Get the destination address.
     * @return destination address.
     */
    public Address getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * Get the message content.
     * @return content of this message
     */
    public Object getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Message{" + "sourceAddress=" + sourceAddress + ", destinationAddress=" + destinationAddress + ", message=" + message + '}';
    }

}
