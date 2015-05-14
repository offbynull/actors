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
package com.offbynull.peernetic.core.gateways.recorder;

import com.offbynull.peernetic.core.shuttle.Address;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * A recorded message. Publicly exposed because other tools may want to read out messages written by {@link RecorderGateway}, or write
 * messages to be read by {@link ReplayerGateway}.
 * @author Kasra Faghihi
 * @see RecordedBlock
 */
public final class RecordedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Address srcAddress;
    private Address dstSuffix;
    private Object message;

    /**
     * Constructs a {@link RecordedMessage} object.
     * @param srcAddress source address of the recorded message
     * @param dstSuffix destination address suffix of the recorded message ({@code null} means no suffix}
     * @param message message being recorded
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code srcAddress} is empty
     */
    public RecordedMessage(Address srcAddress, Address dstSuffix, Object message) {
        Validate.notNull(srcAddress);
        Validate.notNull(dstSuffix);
        Validate.isTrue(!srcAddress.isEmpty());
        Validate.notNull(message);
        this.srcAddress = srcAddress;
        this.dstSuffix = dstSuffix;
        this.message = message;
    }
    
    /**
     * Get the source address.
     * @return source address
     */
    public Address getSrcAddress() {
        return srcAddress;
    }

    /**
     * Get the destination address suffix.
     * @return destination address
     */
    public Address getDstSuffix() {
        return dstSuffix;
    }

    /**
     * Get the message.
     * @return message
     */
    public Object getMessage() {
        return message;
    }
    
}
