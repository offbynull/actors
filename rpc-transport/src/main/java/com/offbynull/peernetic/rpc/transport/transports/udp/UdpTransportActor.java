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
package com.offbynull.peernetic.rpc.transport.transports.udp;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.common.concurrent.actor.SelectorActorQueueNotifier;
import com.offbynull.peernetic.rpc.transport.IncomingMessageListener;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.IncomingMessageManager.IncomingPacketManagerResult;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager.OutgoingMessageManagerResult;
import com.offbynull.peernetic.rpc.transport.internal.OutgoingMessageManager.Packet;
import com.offbynull.peernetic.rpc.transport.internal.TransportActor;
import com.offbynull.peernetic.rpc.transport.internal.PacketBasedTransportImplementationUtils;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

final class UdpTransportActor extends TransportActor<InetSocketAddress> {

    private InetSocketAddress listenAddress;
    private Selector selector;
    private int selectionKey;

    private IncomingMessageListener<InetSocketAddress> incomingMessageListener;
    
    private long packetFlushTimeout;
    private long outgoingResponseTimeout;
    private long incomingResponseTimeout;
    
    private DatagramChannel channel;

    private OutgoingMessageManager<InetSocketAddress> outgoingMessageManager;
    private IncomingMessageManager<InetSocketAddress> incomingMessageManager;
    private long nextId;

    private ByteBuffer buffer;
        
    private int cacheSize;

    public UdpTransportActor(InetSocketAddress listenAddress, int bufferSize, int cacheSize, long packetFlushTimeout,
            long outgoingResponseTimeout, long incomingResponseTimeout) throws IOException {
        super(true);
        
        Validate.notNull(listenAddress);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bufferSize);
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, cacheSize);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, packetFlushTimeout);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, outgoingResponseTimeout);
        Validate.inclusiveBetween(1L, Long.MAX_VALUE, incomingResponseTimeout);

        this.listenAddress = listenAddress;

        this.packetFlushTimeout = packetFlushTimeout;
        this.outgoingResponseTimeout = outgoingResponseTimeout;
        this.incomingResponseTimeout = incomingResponseTimeout;
        
        this.buffer = ByteBuffer.allocate(bufferSize);
        
        this.cacheSize = cacheSize;
    }

    @Override
    protected ActorQueue createQueue() {
        try {
            this.selector = Selector.open();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return new ActorQueue(new SelectorActorQueueNotifier(selector));
    }

    @Override
    protected void onStart() throws Exception {
        outgoingMessageManager = new OutgoingMessageManager<>(getOutgoingFilter()); 
        incomingMessageManager = new IncomingMessageManager<>(cacheSize, getIncomingFilter());
        incomingMessageListener = getIncomingMessageListener();

        try {
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
    }

    @Override
    protected long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception {
        // process commands
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            PacketBasedTransportImplementationUtils.processActorCommand(timestamp, nextId++, msg, outgoingMessageManager,
                    incomingMessageManager, packetFlushTimeout, outgoingResponseTimeout);
        }



        // process timeouts for outgoing requests
        OutgoingMessageManagerResult ommResult = PacketBasedTransportImplementationUtils.processOutgoing(timestamp, outgoingMessageManager);


        
        // go through selected keys
        Iterator keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = (SelectionKey) keys.next();
            keys.remove();

            if (!key.isValid()) {
                continue;
            }

            if (key.isReadable()) { // incoming data available
                buffer.clear();

                InetSocketAddress from = (InetSocketAddress) channel.receive(buffer);
                buffer.flip();
                
                long id = nextId++;
                incomingMessageManager.incomingData(id, from, buffer, timestamp + incomingResponseTimeout);
            } else if (key.isWritable()) { // ready for outgoing data
                Packet<InetSocketAddress> outgoingPacket = outgoingMessageManager.getNextOutgoingPacket();
                if (outgoingPacket == null) {
                    continue;
                }

                channel.send(outgoingPacket.getData(), outgoingPacket.getTo());
            }
        }



        // process timeouts for incoming requests
        IncomingPacketManagerResult<InetSocketAddress> immResult = PacketBasedTransportImplementationUtils.processIncoming(timestamp,
                incomingMessageManager, outgoingMessageManager, incomingMessageListener, getSelfWriter());



        // set selection key based on if there's messages to go out
        int newSelectionKey = SelectionKey.OP_READ;
        if (ommResult.getPacketsAvailable() > 0) {
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
        

        // calculate max time until next invoke
        long waitTime = Long.MAX_VALUE;
        waitTime = Math.min(waitTime, immResult.getMaxTimestamp());
        waitTime = Math.min(waitTime, ommResult.getMaxTimestamp());

        return waitTime;
    }

    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        IOUtils.closeQuietly(selector);
        IOUtils.closeQuietly(channel);

        PacketBasedTransportImplementationUtils.shutdownNotify(outgoingMessageManager);
    }
}