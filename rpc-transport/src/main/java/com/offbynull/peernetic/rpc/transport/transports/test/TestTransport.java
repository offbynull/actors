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
package com.offbynull.peernetic.rpc.transport.transports.test;

import com.offbynull.peernetic.actor.ActorQueue;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.rpc.transport.Deserializer;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import com.offbynull.peernetic.rpc.transport.Serializer;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager.InMessage;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager.OutMessage;
import com.offbynull.peernetic.rpc.transport.internal.SendMessageCommand;
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
     * @param hub hub backing this transport (must be started)
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalStateException if {@code hub} is not started
     */
    public TestTransport(A address, TestHub<A> hub) {
        Validate.notNull(address);
        Validate.notNull(hub);

        this.address = address;
        this.hubEndpoint = hub.getEndpoint();
        
        Validate.isTrue(hubEndpoint != null);
    }

    @Override
    protected ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        OutgoingFilter<A> outgoingFilter = (OutgoingFilter<A>) initVars.get(OUTGOING_FILTER_KEY);
        IncomingFilter<A> incomingFilter = (IncomingFilter<A>) initVars.get(INCOMING_FILTER_KEY);
        routeToEndpoint = (Endpoint) initVars.get(ENDPOINT_ROUTE_KEY);
        Serializer serializer = (Serializer) initVars.get(SERIALIZER_KEY);
        Deserializer deserializer = (Deserializer) initVars.get(DESERIALIZER_KEY);
        
        outgoingMessageManager = new OutgoingMessageManager<>(outgoingFilter, serializer); 
        incomingMessageManager = new IncomingMessageManager<>(incomingFilter, deserializer);

        pushQueue.push(hubEndpoint, new ActivateEndpointCommand<>(address, getEndpoint()));
        
        return new ActorQueue();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue) throws Exception {
        // process commands
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();

            if (content instanceof ReceivePacketFromHubEvent) {
                // msg from hub saying a packet has come in
                ReceivePacketFromHubEvent<A> rpfhe = (ReceivePacketFromHubEvent) content;
                incomingMessageManager.queue(rpfhe.getFrom(), rpfhe.getData(), timestamp);
                //pushQueue.push(routeToEndpoint, new SendPacketToHubCommand(address, smc.getDestination(), smc.getContent()));
            } else if (content instanceof SendMessageCommand) {
                // msg from user saying send out a packet
                SendMessageCommand<A> smc = (SendMessageCommand) content;
                outgoingMessageManager.queue(smc.getDestination(), smc.getContent(), timestamp);
//                
            }
        }
        
        
        Collection<InMessage<A>> inMessages = incomingMessageManager.flush();
        for (InMessage<A> inMessage : inMessages) {
            pushQueue.push(routeToEndpoint, inMessage.getContent());
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