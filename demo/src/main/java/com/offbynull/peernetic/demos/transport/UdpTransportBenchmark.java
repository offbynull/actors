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
package com.offbynull.peernetic.demos.transport;

import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueReader;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueueWriter;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.rpc.transport.transports.udp.UdpTransportFactory;
import com.offbynull.peernetic.rpc.transport.internal.RequestArrivedEvent;
import com.offbynull.peernetic.rpc.transport.internal.ResponseArrivedEvent;
import com.offbynull.peernetic.rpc.transport.internal.ResponseErroredEvent;
import com.offbynull.peernetic.rpc.transport.internal.SendRequestCommand;
import com.offbynull.peernetic.rpc.transport.internal.SendResponseCommand;
import com.offbynull.peernetic.rpc.transport.internal.TransportActor;
import com.offbynull.peernetic.rpc.transport.transports.udp.UdpTransport;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Benchmarks {@link UdpTransport}.
 * @author Kasra Faghihi
 */
public final class UdpTransportBenchmark {
    private static final int NUM_OF_TRANSPORTS = 30;
    
    private UdpTransportBenchmark() {
        // do nothing
    }
    
    /**
     * Entry-point.
     * @param args unused
     * @throws Throwable on error
     */
    public static void main(String[] args) throws Throwable {
        Map<InetSocketAddress, TransportActor<InetSocketAddress>> transports = new HashMap<>();
        Map<InetSocketAddress, ActorQueueWriter> writers = new HashMap<>();


        ActorQueue mainQueue = new ActorQueue();
        ActorQueueWriter mainWriter = mainQueue.getWriter();
        ActorQueueReader mainReader = mainQueue.getReader();
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            final UdpTransportFactory udpTransportFactory = new UdpTransportFactory();
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
            udpTransportFactory.setListenAddress(addr);
            TransportActor transport = udpTransportFactory.createTransport();
            
            transport.setDestinationWriter(mainWriter);
            transport.start();
            
            writers.put(addr, transport.getWriter());
            transports.put(addr, transport);
        }
        
        long id = 0;
        for (int i = 0; i < NUM_OF_TRANSPORTS; i++) {
            InetSocketAddress fromAddr = new InetSocketAddress(InetAddress.getLocalHost(), 10000 + i);
            ActorQueueWriter fromWriter = writers.get(fromAddr);
            for (int j = 0; j < NUM_OF_TRANSPORTS; j++) {
                if (i == j) {
                    continue;
                }
                
                InetSocketAddress toAddr = new InetSocketAddress(InetAddress.getLocalHost(),
                        10000 + ((i + 1) % NUM_OF_TRANSPORTS)); // NOPMD
                
                sendTimestamp(fromWriter, id, mainWriter, toAddr);
            }
        }
        
        int count = 0;
        long accumulatedTime = 0L;
        
        while (true) {
            Iterator<Message> msgIt = mainReader.pull(0L);
            while (msgIt.hasNext()) {
                Message msg = msgIt.next();
                Object content = msg.getContent();

                if (content instanceof RequestArrivedEvent) {
                    RequestArrivedEvent<InetSocketAddress> rae = (RequestArrivedEvent<InetSocketAddress>) content;
                    SendResponseCommand<InetSocketAddress> src = new SendResponseCommand<>(rae.getData());
                    msg.getResponder().respondImmediately(src);
                } else if (content instanceof ResponseArrivedEvent) {
                    ResponseArrivedEvent<InetSocketAddress> rae = (ResponseArrivedEvent<InetSocketAddress>) content;
                    long oldTime = rae.getData().getLong();
                    
                    count++;
                    accumulatedTime += System.currentTimeMillis() - oldTime;
                    if (count == 10000) {
                        System.out.println(accumulatedTime / 10000L);
                        count = 0;
                        accumulatedTime = 0L;
                    }
                    
                    InetSocketAddress address = rae.getFrom();
                    ActorQueueWriter writerForAddress = writers.get(address);
                    sendTimestamp(writerForAddress, id, mainWriter, address);
                } else if (content instanceof ResponseErroredEvent) {
                    //ResponseErroredEvent ree = (ResponseErroredEvent) content;
                    System.out.println("TIMEOUT");
                }
            }
        }
    }

    private static void sendTimestamp(ActorQueueWriter src, long id, ActorQueueWriter dst, InetSocketAddress address) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, System.currentTimeMillis());
        src.push(Message.createRespondableMessage(id, dst, new SendRequestCommand(address, buffer)));
        id++;
    }
}
