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
import com.offbynull.peernetic.common.concurrent.actor.Message.MessageResponder;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.DropResponseCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.commands.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.actormessages.events.RequestArrivedEvent;
import com.offbynull.peernetic.rpc.transport.actormessages.events.ResponseArrivedEvent;
import com.offbynull.peernetic.rpc.transport.actormessages.events.ResponseErroredEvent;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingPacketManagerResult;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingRequest;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.common.IncomingMessageManager.PendingRequest;
import com.offbynull.peernetic.rpc.transport.common.MessageId;
import com.offbynull.peernetic.rpc.transport.common.OutgoingMessageManager;
import com.offbynull.peernetic.rpc.transport.common.OutgoingMessageManager.OutgoingPacketManagerResult;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

/**
 * A {@link Transport} used for testing. Backed by a {@link TestHub}.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class TestTransport<A> extends Transport<A> {

    private A address;

    private long timeout;
    
    private OutgoingMessageManager<A> outgoingPacketManager;
    private IncomingMessageManager<A> incomingPacketManager;
    private long nextId;
    
    private int cacheSize;
    
    private ActorQueueWriter dstWriter;
    private ActorQueueWriter hubWriter;

    /**
     * Constructs a {@link TestTransport} object.
     * @param address address to listen on
     * @param bufferSize buffer size
     * @param cacheSize number of packet ids to cache
     * @param timeout timeout duration
     * @throws IOException on error
     * @throws IllegalArgumentException if port is out of range, or if any of the other arguments are {@code <= 0};
     * @throws NullPointerException if any arguments are {@code null}
     */
    public TestTransport(A address, int cacheSize, long timeout, ActorQueueWriter hubWriter) throws IOException {
        super(true);
        
        Validate.notNull(address);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, cacheSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, timeout);
        Validate.notNull(hubWriter);

        this.address = address;

        this.timeout = timeout;
        
        this.cacheSize = cacheSize;
        this.hubWriter = hubWriter;
    }

    @Override
    protected ActorQueue createQueue() {
        return new ActorQueue();
    }

    @Override
    protected void onStart() throws Exception {
        dstWriter = getDestinationWriter();
        Validate.validState(dstWriter != null);
        
        outgoingPacketManager = new OutgoingMessageManager<>(65535, getOutgoingFilter()); 
        incomingPacketManager = new IncomingMessageManager<>(cacheSize, getIncomingFilter());

        // bind to testhub here
        Message msg = Message.createOneWayMessage(new ActivateEndpointCommand<>(address, getSelfWriter()));
        hubWriter.push(msg);
    }
    
    @Override
    protected long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception {
        // process messages
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            Object content = msg.getContent();
            
            if (content instanceof SendRequestCommand) {
                SendRequestCommand<A> src = (SendRequestCommand) content;
                MessageResponder responder = msg.getResponder();
                if (responder == null) {
                    continue;
                }
                
                long id = nextId++;
                outgoingPacketManager.outgoingRequest(id, src.getTo(), src.getData(), timestamp + timeout, timestamp + timeout, responder);
            } else if (content instanceof SendResponseCommand) {
                SendResponseCommand<A> src = (SendResponseCommand) content;
                Long id = msg.getResponseToId(Long.class);
                if (id == null) {
                    continue;
                }
                
                PendingRequest<A> pendingRequest = incomingPacketManager.responseFormed(id);
                if (pendingRequest == null) {
                    continue;
                }
                
                outgoingPacketManager.outgoingResponse(id, pendingRequest.getFrom(), src.getData(), pendingRequest.getMessageId(),
                        timestamp + timeout);
            } else if (content instanceof DropResponseCommand) {
                DropResponseCommand trc = (DropResponseCommand) content;
                Long id = msg.getResponseToId(Long.class);
                if (id == null) {
                    continue;
                }
                
                incomingPacketManager.responseFormed(id);
            } else if (content instanceof SendMessageCommand) {
                SendMessageCommand<A> imc = (SendMessageCommand) content;
                
                long id = nextId++;
                incomingPacketManager.incomingData(id, imc.getFrom(), imc.getData(), timestamp + timeout);
            }
        }
        
        
        
        // process timeouts for outgoing requests
        OutgoingPacketManagerResult opmResult = outgoingPacketManager.process(timestamp);
        
        Collection<MessageResponder> requestNotSentOut = opmResult.getMessageRespondersForFailures();
        for (MessageResponder responder : requestNotSentOut) {
            responder.respondDeferred(pushQueue, new ResponseErroredEvent());
        }

        
        
        int packetsAvailable = opmResult.getPacketsAvailable();
        for (int i = 0; i < packetsAvailable; i++) {
            ActivateEndpointCommand<A> cmd = new ActivateEndpointCommand<>(address, getSelfWriter());
            pushQueue.queueOneWayMessage(hubWriter, cmd);
        }
        
        
        
        // process timeouts for incoming requests
        IncomingPacketManagerResult<A> ipmResult = incomingPacketManager.process(timestamp);
        
        for (IncomingRequest<A> incomingRequest : ipmResult.getNewIncomingRequests()) {
            RequestArrivedEvent<A> event = new RequestArrivedEvent<>(
                    incomingRequest.getFrom(),
                    incomingRequest.getData(),
                    timestamp);
            pushQueue.queueRespondableMessage(dstWriter, incomingRequest.getId(), event);
        }
        
        for (IncomingResponse<A> incomingResponse : ipmResult.getNewIncomingResponses()) {
            MessageId messageId = incomingResponse.getMessageId();
            MessageResponder responder = outgoingPacketManager.responseReturned(messageId);
            
            if (responder == null) {
                continue;
            }
            
            ResponseArrivedEvent<A> event = new ResponseArrivedEvent<>(
                    incomingResponse.getFrom(),
                    incomingResponse.getData(),
                    timestamp);
            responder.respondDeferred(pushQueue, event);
        }
        
        
        // calculate max time until next invoke
        long waitTime = Long.MAX_VALUE;
        waitTime = Math.max(waitTime, opmResult.getNextTimeoutTimestamp());
        waitTime = Math.max(waitTime, ipmResult.getNextTimeoutTimestamp());
        
        return waitTime;
    }

    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        for (MessageResponder responder : outgoingPacketManager.process(Long.MAX_VALUE).getMessageRespondersForFailures()) {
            ResponseErroredEvent ree = new ResponseErroredEvent();
            responder.respondDeferred(pushQueue, ree);
        }
        
        Message msg = Message.createOneWayMessage(new DeactivateEndpointCommand<>(address));
        hubWriter.push(msg);
    }

}
