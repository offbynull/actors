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

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager.IncomingPacketManagerResult;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager.OutgoingMessageManagerResult;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager.Packet;
import com.offbynull.peernetic.rpc.transport.internal.TransportActor;
import com.offbynull.peernetic.rpc.transport.internal.PacketBasedTransportImplementationUtils;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

final class TestTransportActor<A> extends TransportActor<A> {

    private A address;

    private IncomingMessageListener<A> incomingMessageListener;
    
    private long outgoingResponseTimeout;
    private long incomingResponseTimeout;

    private OutgoingMessageManager<A> outgoingMessageManager;
    private IncomingMessageManager<A> incomingMessageManager;
    private long nextId;

    private int cacheSize;

    private ActorQueueWriter hubWriter;

    public TestTransportActor(A address, int cacheSize, long outgoingResponseTimeout,
            long incomingResponseTimeout, ActorQueueWriter hubWriter) {
        super(true);

        Validate.notNull(address);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, cacheSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, outgoingResponseTimeout);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, incomingResponseTimeout);
        Validate.notNull(hubWriter);

        this.address = address;

        this.outgoingResponseTimeout = outgoingResponseTimeout;
        this.incomingResponseTimeout = incomingResponseTimeout;

        this.cacheSize = cacheSize;
        this.hubWriter = hubWriter;
    }

    @Override
    protected ActorQueue createQueue() {
        return new ActorQueue();
    }

    @Override
    protected void onStart() throws Exception {
        outgoingMessageManager = new OutgoingMessageManager<>(getOutgoingFilter()); 
        incomingMessageManager = new IncomingMessageManager<>(cacheSize, getIncomingFilter());
        incomingMessageListener = getIncomingMessageListener();
        

        // bind to testhub here
        Message msg = Message.createOneWayMessage(new ActivateEndpointCommand<>(address, getSelfWriter()));
        hubWriter.push(msg);
    }

    @Override
    protected long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception {
        // process commands
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            Object content = msg.getContent();

            if (content instanceof ReceiveMessageEvent) {
                ReceiveMessageEvent<A> rme = (ReceiveMessageEvent) content;

                long id = nextId++;
                incomingMessageManager.incomingData(id, rme.getFrom(), rme.getData(), timestamp + incomingResponseTimeout);
            } else {
                PacketBasedTransportImplementationUtils.processActorCommand(timestamp, nextId++, msg, outgoingMessageManager,
                        incomingMessageManager, 1L, outgoingResponseTimeout);
            }
        }



        // process timeouts for outgoing requests
        OutgoingMessageManagerResult ommResult = PacketBasedTransportImplementationUtils.processOutgoing(timestamp, outgoingMessageManager);

        

        Packet<A> packet;
        while ((packet = outgoingMessageManager.getNextOutgoingPacket()) != null) {
            // forward outgoing packet to hub
            SendMessageCommand<A> smc = new SendMessageCommand(address, packet.getTo(), packet.getData());
            pushQueue.queueOneWayMessage(hubWriter, smc);
        }



        // process timeouts for incoming requests
        IncomingPacketManagerResult<A> immResult = PacketBasedTransportImplementationUtils.processIncoming(timestamp,
                incomingMessageManager, outgoingMessageManager, incomingMessageListener, getSelfWriter());



        // calculate max time until next invoke
        long waitTime = Long.MAX_VALUE;
        waitTime = Math.min(waitTime, immResult.getMaxTimestamp());
        waitTime = Math.min(waitTime, ommResult.getMaxTimestamp());

        return waitTime;
    }

    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        Message msg = Message.createOneWayMessage(new DeactivateEndpointCommand<>(address));
        hubWriter.push(msg);

        PacketBasedTransportImplementationUtils.shutdownNotify(outgoingMessageManager);
    }
}