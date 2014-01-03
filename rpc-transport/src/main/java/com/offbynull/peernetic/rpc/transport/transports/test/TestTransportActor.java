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

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.internal.DefaultIncomingResponseListener;
import com.offbynull.peernetic.rpc.transport.internal.DropResponseCommand;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.MessageId;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.internal.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.internal.TransportActor;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

final class TestTransportActor<A> extends TransportActor<A> {

    private A address;

    private IncomingMessageListener<A> incomingMessageListener;
    
    private long outgoingResponseTimeout;
    private long incomingResponseTimeout;

    private OutgoingMessageManager<A> outgoingPacketManager;
    private IncomingMessageManager<A> incomingPacketManager;
    private long nextId;

    private int cacheSize;

    private ActorQueueWriter hubWriter;

    /**
     * Constructs a {@link TestTransport} object.
     * @param address address to listen on
     * @param cacheSize number of packet ids to cache
     * @param outgoingResponseTimeout timeout duration for responses for outgoing requests to arrive
     * @param incomingResponseTimeout timeout duration for responses for incoming requests to be processed
     * @param hubWriter writer to {@link TestHub}
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     * @throws NullPointerException if any arguments are {@code null}
     */
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
    protected void onStart() throws Exception {
        outgoingPacketManager = new OutgoingMessageManager<>(getOutgoingFilter()); 
        incomingPacketManager = new IncomingMessageManager<>(cacheSize, getIncomingFilter());
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

            if (content instanceof SendRequestCommand) {
                SendRequestCommand<A> src = (SendRequestCommand) content;
                OutgoingMessageResponseListener listener = src.getListener();

                long id = nextId++;
                outgoingPacketManager.outgoingRequest(id, src.getTo(), src.getData(), timestamp + 1L,
                        timestamp + outgoingResponseTimeout, listener);
            } else if (content instanceof SendResponseCommand) {
                SendResponseCommand<A> src = (SendResponseCommand) content;
                Long id = msg.getResponseToId(Long.class);
                if (id == null) {
                    continue;
                }

                IncomingMessageManager.IncomingRequestInfo<A> pendingRequest = incomingPacketManager.responseFormed(id);
                if (pendingRequest == null) {
                    continue;
                }

                outgoingPacketManager.outgoingResponse(id, pendingRequest.getFrom(), src.getData(), pendingRequest.getMessageId(),
                        timestamp + 1L);
            } else if (content instanceof DropResponseCommand) {
                //DropResponseCommand trc = (DropResponseCommand) content;
                Long id = msg.getResponseToId(Long.class);
                if (id == null) {
                    continue;
                }

                incomingPacketManager.responseFormed(id);
            } else if (content instanceof ReceiveMessageEvent) {
                ReceiveMessageEvent<A> rme = (ReceiveMessageEvent) content;

                long id = nextId++;
                incomingPacketManager.incomingData(id, rme.getFrom(), rme.getData(), timestamp + incomingResponseTimeout);
            }
        }



        // process timeouts for outgoing requests and write new outgoing requests
        OutgoingMessageManager.OutgoingPacketManagerResult opmResult = outgoingPacketManager.process(timestamp);

        Collection<OutgoingMessageResponseListener> listenersForFailures = opmResult.getListenersForFailures();
        for (OutgoingMessageResponseListener outgoingResponseListener : listenersForFailures) {
            try {
                outgoingResponseListener.errorOccurred("Timeout");
            } catch (RuntimeException re) { // NOPMD
            }
        }

        OutgoingMessageManager.Packet<A> packet;
        while ((packet = outgoingPacketManager.getNextOutgoingPacket()) != null) {
            // forward outgoing packet to hub
            SendMessageCommand<A> smc = new SendMessageCommand(address, packet.getTo(), packet.getData());
            pushQueue.queueOneWayMessage(hubWriter, smc);
        }



        // process timeouts for incoming requests
        IncomingMessageManager.IncomingPacketManagerResult<A> ipmResult = incomingPacketManager.process(timestamp);

        for (IncomingMessageManager.IncomingRequest<A> incomingRequest : ipmResult.getNewIncomingRequests()) {
            IncomingMessageResponseListener incomingMessageResponseListener =
                    new DefaultIncomingResponseListener(incomingRequest.getId(), getSelfWriter());
            incomingMessageListener.messageArrived(incomingRequest.getFrom(), incomingRequest.getData(), incomingMessageResponseListener);
        }

        for (IncomingMessageManager.IncomingResponse<A> incomingResponse : ipmResult.getNewIncomingResponses()) {
            MessageId messageId = incomingResponse.getMessageId();
            OutgoingMessageResponseListener outgoingMessageResponseListener = outgoingPacketManager.responseReturned(messageId);

            if (outgoingMessageResponseListener == null) {
                continue;
            }

            try {
                outgoingMessageResponseListener.responseArrived(incomingResponse.getData());
            } catch (RuntimeException re) { // NOPMD
            }
        }


        // calculate max time until next invoke
        long waitTime = Long.MAX_VALUE;
        waitTime = Math.min(waitTime, opmResult.getMaxTimestamp());
        waitTime = Math.min(waitTime, ipmResult.getMaxTimestamp());

        return waitTime;
    }

    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        for (OutgoingMessageResponseListener listener : outgoingPacketManager.process(Long.MAX_VALUE).getListenersForFailures()) {
            try {
                listener.errorOccurred("Shutdown");
            } catch (RuntimeException re) { // NOPMD
            }
        }

        Message msg = Message.createOneWayMessage(new DeactivateEndpointCommand<>(address));
        hubWriter.push(msg);
    }
}