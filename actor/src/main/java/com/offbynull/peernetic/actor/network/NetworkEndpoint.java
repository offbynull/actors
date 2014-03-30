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
import io.netty.channel.Channel;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import sun.rmi.transport.Transport;

/**
 * An endpoint that points that proxies a {@link Transport}'s endpoint but also adds an address. Use to target a specific address over a
 * network.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class NetworkEndpoint<A> implements Endpoint {
    private Channel channel;
    private A address;

    /**
     * Construct a {@link NetworkEndpoint} object.
     * @param channel channel to shuttle objects to
     * @param address address to point to
     */
    public NetworkEndpoint(Channel channel, A address) {
        Validate.notNull(channel);
        Validate.notNull(address);
        Validate.isTrue(channel instanceof SocketChannel || channel instanceof DatagramChannel);
        
        this.channel = channel;
        this.address = address;
    }

    @Override
    public void push(Endpoint source, Collection<Outgoing> outgoing) {
        Validate.notNull(source);
        Validate.noNullElements(outgoing);
        
        if (channel instanceof DatagramChannel) {
            for (Outgoing outgoingMsg : outgoing) {
                Object originalContent = outgoingMsg.getContent();

                Object nettyMsg = new DefaultAddressedEnvelope<>(originalContent, (SocketAddress) address);
                channel.write(nettyMsg);
            }
        } else if (channel instanceof SocketChannel) {
            
        } else {
            throw new IllegalStateException();
        }
        
        channel.flush();
    }

    @Override
    public void push(Endpoint source, Outgoing... outgoing) {
        push(source, Arrays.asList(outgoing));
    }

    A getAddress() {
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
        final NetworkEndpoint<?> other = (NetworkEndpoint<?>) obj;
        if (!Objects.equals(this.channel, other.channel)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }
    
}
