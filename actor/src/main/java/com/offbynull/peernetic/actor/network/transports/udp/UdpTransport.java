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
package com.offbynull.peernetic.actor.network.transports.udp;

import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.actor.network.Deserializer;
import com.offbynull.peernetic.actor.network.IncomingFilter;
import com.offbynull.peernetic.actor.network.NetworkEndpoint;
import com.offbynull.peernetic.actor.network.OutgoingFilter;
import com.offbynull.peernetic.actor.network.Serializer;
import com.offbynull.peernetic.actor.network.Transport;
import com.offbynull.peernetic.actor.network.internal.IncomingMessageManager;
import com.offbynull.peernetic.actor.network.internal.IncomingMessageManager.InMessage;
import com.offbynull.peernetic.actor.network.internal.OutgoingMessageManager;
import com.offbynull.peernetic.actor.network.internal.OutgoingMessageManager.OutMessage;
import com.offbynull.peernetic.actor.network.internal.SendMessageCommand;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

/**
 * A UDP transport implementation.
 * @author Kasra Faghihi
 */
public final class UdpTransport extends Transport<InetSocketAddress> {

    private InetSocketAddress listenAddress;

    private OutgoingMessageManager<InetSocketAddress> outgoingMessageManager;
    private IncomingMessageManager<InetSocketAddress> incomingMessageManager;
    
    private DatagramChannel channel;
    private Selector selector;
    private int selectionKey;
    
    private ByteBuffer recvBuffer;

    /**
     * Constructs a {@link UdpTransport} object.
     * @param listenAddress listen address of this transport
     * @param recvBufferSize size of buffer used to read UDP packets
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if any numeric arguments are negative
     */
    public UdpTransport(InetSocketAddress listenAddress, int recvBufferSize) {
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, recvBufferSize);

        this.listenAddress = listenAddress;
        this.recvBuffer = ByteBuffer.allocate(recvBufferSize);
    }

    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        OutgoingFilter<InetSocketAddress> outgoingFilter = (OutgoingFilter<InetSocketAddress>) initVars.get(OUTGOING_FILTER_KEY);
        IncomingFilter<InetSocketAddress> incomingFilter = (IncomingFilter<InetSocketAddress>) initVars.get(INCOMING_FILTER_KEY);
        Serializer serializer = (Serializer) initVars.get(SERIALIZER_KEY);
        Deserializer deserializer = (Deserializer) initVars.get(DESERIALIZER_KEY);
        
        outgoingMessageManager = new OutgoingMessageManager<>(outgoingFilter, serializer); 
        incomingMessageManager = new IncomingMessageManager<>(incomingFilter, deserializer);
        
        try {
            selector = Selector.open();
            selectionKey = SelectionKey.OP_READ;

            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.register(selector, selectionKey);
            channel.socket().bind(listenAddress);
        } catch (RuntimeException | IOException e) {
            IOUtils.closeQuietly(selector);
            IOUtils.closeQuietly(channel);
            throw e;
        }
        
        return new ActorStartSettings(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        // process managers
        long immNextTimeoutTimestamp = incomingMessageManager.process(timestamp);
        long ommNextTimeoutTimestamp = outgoingMessageManager.process(timestamp);
        
        // add messages to outgoing queue
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Object content = incoming.getContent();

            if (content instanceof SendMessageCommand) {
                // msg from user saying send out a packet
                SendMessageCommand<InetSocketAddress> smc = (SendMessageCommand) content;
                outgoingMessageManager.queue(smc.getDestination(), smc.getContent(), timestamp + 100L);
            } else {
                throw new IllegalStateException();
            }
        }
        
        
        // go through selected keys
        Iterator keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = (SelectionKey) keys.next();
            keys.remove();

            if (!key.isValid()) {
                continue;
            }

            if (key.isReadable()) {
                // read in a packet and push it in as a message to the incoming queue
                recvBuffer.clear();
                InetSocketAddress from = (InetSocketAddress) channel.receive(recvBuffer);
                recvBuffer.flip();
                incomingMessageManager.queue(from, recvBuffer, timestamp + 1L);
            } else if (key.isWritable()) {
                // pull out a message from the outgoing queue and write it in as a packet
                OutMessage<InetSocketAddress> outMessage = outgoingMessageManager.getNext();
                if (outMessage.getData() == null) {
                    continue;
                }
                channel.send(outMessage.getData(), outMessage.getTo());
            }
        }
        
        
        // flush all messages in the incoming queue and push them out
        Endpoint dstEndpoint = getDestinationEndpoint();
        Collection<InMessage<InetSocketAddress>> inMessages = incomingMessageManager.flush();
        for (InMessage<InetSocketAddress> inMessage : inMessages) {
            NetworkEndpoint<InetSocketAddress> networkEndpoint = new NetworkEndpoint(selfEndpoint, inMessage.getFrom());
            pushQueue.push(networkEndpoint, dstEndpoint, inMessage.getContent());
        }
        
        
        // set selection key based on if there's messages to go out
        int newSelectionKey = SelectionKey.OP_READ;
        if (outgoingMessageManager.hasMore()) {
            newSelectionKey |= SelectionKey.OP_WRITE;
        }

        if (newSelectionKey != selectionKey) {
            selectionKey = newSelectionKey;
            try {
                channel.register(selector, selectionKey);
            } catch (ClosedChannelException cce) {
                throw new RuntimeException(cce);
            }
        }
        
        
        // calculate next wait time
        long nextHitTime = Long.MAX_VALUE;
        nextHitTime = Math.min(nextHitTime, immNextTimeoutTimestamp);
        nextHitTime = Math.min(nextHitTime, ommNextTimeoutTimestamp);
        return nextHitTime;
    }

    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(channel);
    }
}
