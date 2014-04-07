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
package com.offbynull.peernetic.actor.network;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Outgoing;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * An endpoint that dumps messages to a packet-based Netty {@link Channel}. Messages will be wrapped in {@link AddressedEnvelope}.
 * @author Kasra Faghihi
 */
public final class NetworkEndpoint implements Endpoint {
    private Channel channel;
    private SocketAddress address;

    /**
     * Construct a {@link NetworkEndpoint} object.
     * @param channel Netty channel
     * @param address address to point to
     */
    public NetworkEndpoint(Channel channel, SocketAddress address) {
        Validate.notNull(channel);
        Validate.notNull(address);
        
        this.channel = channel;
        this.address = address;
    }

    @Override
    public void push(Endpoint source, Collection<Outgoing> outgoing) {
        Validate.notNull(source);
        Validate.noNullElements(outgoing);
        
        for (Outgoing outgoingMsg : outgoing) {
            Object content = outgoingMsg.getContent();
            channel.write(new DefaultAddressedEnvelope<>(content, address));
        }
        
        channel.flush();
    }

    @Override
    public void push(Endpoint source, Outgoing... outgoing) {
        push(source, Arrays.asList(outgoing));
    }

    SocketAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.channel);
        hash = 89 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NetworkEndpoint other = (NetworkEndpoint) obj;
        if (!Objects.equals(this.channel, other.channel)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
    
}
