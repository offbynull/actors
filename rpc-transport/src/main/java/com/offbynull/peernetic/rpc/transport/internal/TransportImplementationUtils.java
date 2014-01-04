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
package com.offbynull.peernetic.rpc.transport.internal;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.IncomingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.OutgoingMessageResponseListener;
import com.offbynull.peernetic.rpc.transport.Transport;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager.IncomingPacketManagerResult;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager.IncomingRequest;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager.IncomingResponse;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager.OutgoingMessageManagerResult;
import java.util.Collection;
import org.apache.commons.lang3.Validate;

/**
 * A utility class that contains common code for {@link Transport} implementations.
 * @author Kasra Faghihi
 */
public final class TransportImplementationUtils {
    private TransportImplementationUtils() {
        
    }
    
    /**
     * Processes transport-related actor commands: {@link SendRequestCommand}, {@link SendResponseCommand}, and {@link DropResponseCommand}.
     * For each actor command, the appropriate functionality is called on {@code incomingMessageManager} / {@code outgoingMessageManager}.
     * <p/>
     * <ul>
     * <li>{@link SendRequestCommand} - registers an outgoing request with {@code outgoingMessageManager} and pushes it on to
     * {@code outgoingMessageManager}'s send queue</li>
     * <li>{@link SendResponseCommand} - deregisters the incoming request the response is for from {@code incomingMessageManager} and pushes
     * the response on to {@code outgoingMessageManager}'s send queue.</li>
     * <li>{@link DropResponseCommand} - deregisters the incoming request the response is for from {@code incomingMessageManager}</li>
     * </ul>
     * @param <A> address type
     * @param timestamp current time
     * @param nextAvailableId next id that can be used to reference a command to {@code incomingMessageManager} or
     * {@code outgoingMessageManager}
     * @param msg message to be processed
     * @param outgoingMessageManager outgoing message manager for the transport
     * @param incomingMessageManager incoming message manager for the transport
     * @param messageFlushTimeoutDuration maximum amount of time to wait for an outgoing request/response to flush to the network before
     * dropping it and reporting an error
     * @param messageResponseTimeoutDuration maximum amount of time to wait for a response for an outgoing request before reporting an error
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument other than {@code timestamp} is non-positive (less than 1)
     */
    public static <A> void processActorCommand(long timestamp, long nextAvailableId, Message msg,
            OutgoingMessageManager<A> outgoingMessageManager,
            IncomingMessageManager<A> incomingMessageManager,
            long messageFlushTimeoutDuration,
            long messageResponseTimeoutDuration) {
        Validate.notNull(msg);
        Validate.notNull(outgoingMessageManager);
        Validate.notNull(incomingMessageManager);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, messageFlushTimeoutDuration);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, messageResponseTimeoutDuration);

        Object content = msg.getContent();

        if (content instanceof SendRequestCommand) {
            SendRequestCommand<A> src = (SendRequestCommand) content;
            OutgoingMessageResponseListener listener = src.getListener();

            long id = nextAvailableId;
            outgoingMessageManager.outgoingRequest(id, src.getTo(), src.getData(), timestamp + messageFlushTimeoutDuration,
                    timestamp + messageResponseTimeoutDuration, listener);
        } else if (content instanceof SendResponseCommand) {
            SendResponseCommand<A> src = (SendResponseCommand) content;
            Long id = msg.getResponseToId(Long.class);
            if (id == null) {
                return;
            }

            IncomingMessageManager.IncomingRequestInfo<A> pendingRequest = incomingMessageManager.responseFormed(id);
            if (pendingRequest == null) {
                return;
            }

            outgoingMessageManager.outgoingResponse(id, pendingRequest.getFrom(), src.getData(), pendingRequest.getMessageId(),
                    timestamp + messageFlushTimeoutDuration);
        } else if (content instanceof DropResponseCommand) {
            //DropResponseCommand trc = (DropResponseCommand) content;
            Long id = msg.getResponseToId(Long.class);
            if (id == null) {
                return;
            }

            incomingMessageManager.responseFormed(id);
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Calls {@link IncomingMessageManager#process(long) } on {@code incomingMessageManager}.
     * Goes through all new messages queued in {@code incomingMessageManager} and calls the appropriate callbacks for each. New requests are
     * forwarded to {@code incomingMessageListener}, while responses are forwarded to the response listener of the outgoing request that
     * they're for (if those requests are still around / haven't timed out).
     * @param <A> address type.
     * @param timestamp current timestamp
     * @param outgoingMessageManager outgoing message manager for the transport
     * @param incomingMessageManager incoming message manager for the transport
     * @param incomingMessageListener incoming message listener for the transport
     * @param selfWriter the transport's writer (used to notify the transport of responses to new requests)
     * @return the result of the process call
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <A> IncomingPacketManagerResult<A> processIncoming(long timestamp, IncomingMessageManager incomingMessageManager,
            OutgoingMessageManager outgoingMessageManager, IncomingMessageListener<A> incomingMessageListener,
            ActorQueueWriter selfWriter) {
        Validate.notNull(incomingMessageManager);
        Validate.notNull(outgoingMessageManager);
        Validate.notNull(incomingMessageListener);
        Validate.notNull(selfWriter);
        
        IncomingPacketManagerResult<A> ipmResult = incomingMessageManager.process(timestamp);
 
        // notify of new incoming requests
        for (IncomingRequest<A> incomingRequest : ipmResult.getNewIncomingRequests()) {
            IncomingMessageResponseListener incomingMessageResponseListener =
                    new DefaultIncomingResponseListener(incomingRequest.getId(), selfWriter);
            incomingMessageListener.messageArrived(incomingRequest.getFrom(), incomingRequest.getData(), incomingMessageResponseListener);
        }
 
        // notify of incoming response to outgoing requests
        for (IncomingResponse<A> incomingResponse : ipmResult.getNewIncomingResponses()) {
            MessageId messageId = incomingResponse.getMessageId();
            OutgoingMessageResponseListener outgoingMessageResponseListener = outgoingMessageManager.responseReturned(messageId);
 
            if (outgoingMessageResponseListener == null) {
                continue;
            }
 
            try {
                outgoingMessageResponseListener.responseArrived(incomingResponse.getData());
            } catch (RuntimeException re) { // NOPMD
            }
        }
        
        return ipmResult;
    }
    
    
    /**
     * Calls {@link IncomingMessageManager#process(long) } on {@code incomingMessageManager}.
     * Goes through all outgoing requests that have timed out and calls the error callback for each.
     * @param <A> address type.
     * @param timestamp current timestamp
     * @param outgoingMessageManager outgoing message manager for the transport
     * @return the result of the process call
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <A> OutgoingMessageManagerResult processOutgoing(long timestamp, OutgoingMessageManager outgoingMessageManager) {
        Validate.notNull(outgoingMessageManager);
        
        OutgoingMessageManagerResult ommResult = outgoingMessageManager.process(timestamp);

        Collection<OutgoingMessageResponseListener> listenersForFailures = ommResult.getListenersForFailures();
        for (OutgoingMessageResponseListener outgoingResponseListener : listenersForFailures) {
            try {
                outgoingResponseListener.errorOccurred("Timeout");
            } catch (RuntimeException re) { // NOPMD
            }
        }
        
        return ommResult;
    }

    /**
     * Notifies all requests waiting for a response or waiting to be flushed to the network that the transport shut down.
     * @param <A> address type
     * @param outgoingMessageManager outgoing message manager for the transport
     * @throws NullPointerException if any arguments are {@code null}
     */
    public static <A> void shutdownNotify(OutgoingMessageManager<A> outgoingMessageManager) {
        Validate.notNull(outgoingMessageManager);
        for (OutgoingMessageResponseListener listener : outgoingMessageManager.process(Long.MAX_VALUE).getListenersForFailures()) {
            try {
                listener.errorOccurred("Shutdown");
            } catch (RuntimeException re) { // NOPMD
            }
        }
    }
}
