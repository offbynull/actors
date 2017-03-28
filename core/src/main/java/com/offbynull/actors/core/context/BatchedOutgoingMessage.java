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
package com.offbynull.actors.core.context;

import com.offbynull.actors.core.shuttle.Address;
import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * A queued outgoing message.
 * @author Kasra Faghihi
 */
public final class BatchedOutgoingMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private final Address source;
    private final Address destination;
    private final Object message;

    BatchedOutgoingMessage(Address source, Address destination, Object message) {
        Validate.notNull(source);
        Validate.notNull(destination);
        Validate.notNull(message);
        Validate.isTrue(!destination.isEmpty());
        this.source = source;
        this.destination = destination;
        this.message = message;
    }

    /**
     * Source address of the outgoing message.
     * @return source address
     */
    public Address getSource() {
        return source;
    }

    /**
     * Destination address of the outgoing message.
     * @return destination address
     */
    public Address getDestination() {
        return destination;
    }

    /**
     * Outgoing message.
     * @return outgoing message
     */
    public Object getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.source);
        hash = 47 * hash + Objects.hashCode(this.destination);
        hash = 47 * hash + Objects.hashCode(this.message);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BatchedOutgoingMessage other = (BatchedOutgoingMessage) obj;
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.destination, other.destination)) {
            return false;
        }
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        return true;
    }
    
}
