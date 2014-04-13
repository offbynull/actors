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
import com.offbynull.peernetic.actor.NullEndpoint;
import com.offbynull.peernetic.actor.Outgoing;
import com.offbynull.peernetic.nettyextensions.handlers.readwrite.IncomingMessageListener;
import com.offbynull.peernetic.nettyextensions.handlers.readwrite.Message;
import java.util.Collections;

/**
 * Pushes incoming messages from Netty to an {@link Endpoint}.
 * @author Kasra Faghihi
 */
public final class IncomingMessageToEndpointAdapter implements IncomingMessageListener {
    private volatile Endpoint endpoint = new NullEndpoint();

    /**
     * Set the endpoint to push to.
     * @param endpoint endpoint to push to
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void newMessage(Message incomingMessage) {
        endpoint.push(
                new NetworkEndpoint(incomingMessage.getChannel(), incomingMessage.getRemoteAddress()),
                Collections.singleton(new Outgoing(incomingMessage.getMessage(), endpoint)));
    }
}
