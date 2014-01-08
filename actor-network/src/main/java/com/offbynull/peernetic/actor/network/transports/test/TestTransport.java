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
package com.offbynull.peernetic.actor.network.transports.test;

import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.network.Deserializer;
import com.offbynull.peernetic.actor.network.IncomingFilter;
import com.offbynull.peernetic.actor.network.NetworkEndpoint;
import com.offbynull.peernetic.actor.network.OutgoingFilter;
import com.offbynull.peernetic.actor.network.Serializer;
import com.offbynull.peernetic.actor.network.internal.IncomingMessageManager;
import com.offbynull.peernetic.actor.network.internal.OutgoingMessageManager;
import com.offbynull.peernetic.actor.network.Transport;
import com.offbynull.peernetic.actor.network.internal.IncomingMessageManager.InMessage;
import com.offbynull.peernetic.actor.network.internal.OutgoingMessageManager.OutMessage;
import com.offbynull.peernetic.actor.network.internal.SendMessageCommand;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * A {@link Transport} used for testing. Backed by a {@link TestHub}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class TestTransport<A> extends Transport<A> {

    private A address;

    private OutgoingMessageManager<A> outgoingMessageManager;
    private IncomingMessageManager<A> incomingMessageManager;

    private Endpoint routeToEndpoint;
    private Endpoint hubEndpoint;

    /**
     * Constructs a {@link TestTransport} object.
     * @param address address of this transport
     * @param hubEndpoint endpoint of hub backing this transport (must be started)
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if {@code hub} is not started
     */
    public TestTransport(A address, Endpoint hubEndpoint) {
        Validate.notNull(address);
        Validate.notNull(hubEndpoint);

        this.address = address;
        this.hubEndpoint = hubEndpoint;
    }

    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        OutgoingFilter<A> outgoingFilter = (OutgoingFilter<A>) initVars.get(OUTGOING_FILTER_KEY);
        IncomingFilter<A> incomingFilter = (IncomingFilter<A>) initVars.get(INCOMING_FILTER_KEY);
        routeToEndpoint = (Endpoint) initVars.get(ENDPOINT_ROUTE_KEY);
        Serializer serializer = (Serializer) initVars.get(SERIALIZER_KEY);
        Deserializer deserializer = (Deserializer) initVars.get(DESERIALIZER_KEY);
        
        outgoingMessageManager = new OutgoingMessageManager<>(outgoingFilter, serializer); 
        incomingMessageManager = new IncomingMessageManager<>(incomingFilter, deserializer);

        pushQueue.push(hubEndpoint, new ActivateEndpointCommand<>(address));
        
        return new ActorStartSettings();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        // process commands
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();

            if (content instanceof ReceivePacketFromHubEvent) {
                // msg from hub saying a packet has come in
                ReceivePacketFromHubEvent<A> rpfhe = (ReceivePacketFromHubEvent) content;
                incomingMessageManager.queue(rpfhe.getFrom(), rpfhe.getData(), timestamp);
            } else if (content instanceof SendMessageCommand) {
                // msg from user saying send out a packet
                SendMessageCommand<A> smc = (SendMessageCommand) content;
                outgoingMessageManager.queue(smc.getDestination(), smc.getContent(), timestamp);
            } else {
                throw new IllegalStateException();
            }
        }
        
        
        Collection<InMessage<A>> inMessages = incomingMessageManager.flush();
        for (InMessage<A> inMessage : inMessages) {
            NetworkEndpoint<A> networkEndpoint = new NetworkEndpoint(selfEndpoint, inMessage.getFrom());
            pushQueue.push(networkEndpoint, routeToEndpoint, inMessage.getContent());
        }
        
        Collection<OutMessage<A>> outMessages = outgoingMessageManager.flush();
        for (OutMessage<A> outMessage : outMessages) {
            pushQueue.push(hubEndpoint, new SendPacketToHubCommand(address, outMessage.getTo(), outMessage.getData()));
        }
        
        incomingMessageManager.process(timestamp);
        outgoingMessageManager.process(timestamp);
        
        return Long.MAX_VALUE;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
        pushQueue.push(hubEndpoint, new DeactivateEndpointCommand<>(address));
    }
}