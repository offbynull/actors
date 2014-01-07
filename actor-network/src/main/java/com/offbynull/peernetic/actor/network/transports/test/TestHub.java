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

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorQueue;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.apache.commons.lang3.Validate;

/**
 * A hub that pipes messages between {@link TestTransport}s.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class TestHub<A> extends Actor {

    
    private Line<A> line;
    private PriorityQueue<TransitMessage<A>> transitMessageQueue;
    private Map<A, Endpoint> addressMap;
    
    /**
     * Construct a {@link TestHub} object.
     * @param line line to use
     * @throws NullPointerException if any arguments are {@code null}
     */
    public TestHub(Line<A> line) {
        Validate.notNull(line);

        this.line = line;
    }

    @Override
    protected ActorQueue onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        transitMessageQueue = new PriorityQueue<>(11, new TransitMessageArriveTimeComparator());
        addressMap = new HashMap<>();
        
        return new ActorQueue();
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        // process commands
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();
            
            if (content instanceof ActivateEndpointCommand) {
                ActivateEndpointCommand<A> aec = (ActivateEndpointCommand<A>) content;
                addressMap.put(aec.getAddress(), incoming.getSource());
            } else if (content instanceof DeactivateEndpointCommand) {
                DeactivateEndpointCommand<A> dec = (DeactivateEndpointCommand<A>) content;
                addressMap.remove(dec.getAddress());
            } else if (content instanceof SendPacketToHubCommand) {
                SendPacketToHubCommand<A> imc = (SendPacketToHubCommand<A>) content;
                Collection<TransitMessage<A>> transitMessages = line.depart(imc.getFrom(), imc.getTo(), imc.getData());
                transitMessageQueue.addAll(transitMessages);
            }
        }
        
        // get expired transit messages
        List<TransitMessage<A>> outgoingPackets = new LinkedList<>();

        while (!transitMessageQueue.isEmpty()) {
            TransitMessage<A> topPacket = transitMessageQueue.peek();
            long arriveTime = topPacket.getArriveTime();
            if (arriveTime > timestamp) {
                break;
            }

            outgoingPackets.add(topPacket);
            transitMessageQueue.poll(); // remove
        }

        // pass through line
        Collection<TransitMessage<A>> revisedOutgoingPackets = line.arrive(outgoingPackets);
        
        // notify of events
        for (TransitMessage<A> transitMessage : revisedOutgoingPackets) {
            A to = transitMessage.getTo();
            Endpoint destination = addressMap.get(to);
            
            if (destination == null) {
                continue;
            }
            
            ReceivePacketFromHubEvent<A> rme = new ReceivePacketFromHubEvent<>(transitMessage.getFrom(), transitMessage.getTo(),
                    transitMessage.getData());
            pushQueue.push(destination, rme);
        }
        
        // calculate wait
        TransitMessage<A> nextTransitMessage = transitMessageQueue.peek();
        return nextTransitMessage == null ? Long.MAX_VALUE : nextTransitMessage.getArriveTime();
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }
}
