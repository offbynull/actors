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
package com.offbynull.peernetic.nettyp2p.simulation;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * A hub that pipes messages between {@link LocalDatagramChannel}s.
 *
 * @author Kasra Faghihi
 */
public final class TransitPacketRepository implements Closeable {

    private final Thread loopThread;
    private final Line line;
    private final LinkedBlockingDeque<Object> queue;
    private volatile boolean closed;

    private TransitPacketRepository(Line line) {
        Validate.notNull(line);

        this.loopThread = new Thread(new Loop());
        this.line = line;
        this.queue = new LinkedBlockingDeque<>();
        
        loopThread.setDaemon(true);
        loopThread.setName("Transit Packet Repository");
    }
    
    public static final TransitPacketRepository create(Line line) {
        TransitPacketRepository repo = new TransitPacketRepository(line);
        repo.loopThread.start();
        return repo;
    }

    void registerChannel(InetSocketAddress address, LocalDatagramChannel channel) {
        if (closed) {
            return;
        }
        queue.add(new RegisterCommand(address, channel));
    }

    void unregisterChannel(InetSocketAddress address) {
        if (closed) {
            return;
        }
        queue.add(new UnregisterCommand(address));
    }

    void sendPacket(InetSocketAddress fromAddress, InetSocketAddress toAddress, ByteBuffer data) {
        if (closed) {
            return;
        }
        queue.add(new SendCommand(fromAddress, toAddress, data));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        queue.addFirst(new ShutdownCommand());
        closed = true;
    }

    private final class Loop implements Runnable {

        @Override
        public void run() {
            // initialize
            PriorityQueue<TransitPacket> transitMessageQueue = new PriorityQueue<>(11, new TransitPacketArriveTimeComparator());
            Map<InetSocketAddress, LocalDatagramChannel> addressMap = new HashMap<>();

            // process commands
            long waitTime = Long.MAX_VALUE;
            while (true) {
                Object cmd;
                try {
                    cmd = queue.poll(waitTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                    return;
                }

                long timestamp = System.currentTimeMillis();

                if (cmd instanceof RegisterCommand) {
                    RegisterCommand aec = (RegisterCommand) cmd;
                    addressMap.put(aec.getAddress(), aec.getChannel());
                } else if (cmd instanceof UnregisterCommand) {
                    UnregisterCommand dec = (UnregisterCommand) cmd;
                    addressMap.remove(dec.getAddress());
                } else if (cmd instanceof SendCommand) {
                    SendCommand imc = (SendCommand) cmd;
                    Collection<TransitPacket> transitMessages = line.depart(timestamp, imc.getFrom(), imc.getTo(), imc.getData());
                    transitMessageQueue.addAll(transitMessages);
                } else if (cmd instanceof ShutdownCommand) {
                    return;
                }

                // get expired transit messages
                List<TransitPacket> outgoingPackets = new LinkedList<>();

                while (!transitMessageQueue.isEmpty()) {
                    TransitPacket topPacket = transitMessageQueue.peek();
                    long arriveTime = topPacket.getArriveTime();
                    if (arriveTime > timestamp) {
                        break;
                    }

                    outgoingPackets.add(topPacket);
                    transitMessageQueue.poll(); // remove
                }

                // pass through line
                Collection<TransitPacket> revisedOutgoingPackets = line.arrive(timestamp, outgoingPackets);

                // notify of events
                for (TransitPacket transitMessage : revisedOutgoingPackets) {
                    InetSocketAddress to = transitMessage.getTo();
                    LocalDatagramChannel dst = addressMap.get(to);

                    if (dst == null) {
                        continue;
                    }

                    InetSocketAddress from = transitMessage.getFrom();
                    ByteBuffer msg = transitMessage.getData();
                    dst.triggerRead(from, to, msg);
                }

                // calculate wait
                TransitPacket nextTransitMessage = transitMessageQueue.peek();
                waitTime = nextTransitMessage == null ? Long.MAX_VALUE : nextTransitMessage.getArriveTime() - System.currentTimeMillis();
                waitTime = Math.max(0L, waitTime); // must not be less than 0!
            }
        }
    }

    private static final class RegisterCommand {

        private final InetSocketAddress address;
        private final LocalDatagramChannel channel;

        public RegisterCommand(InetSocketAddress address, LocalDatagramChannel channel) {
            Validate.notNull(address);
            Validate.notNull(channel);

            this.address = address;
            this.channel = channel;
        }

        public InetSocketAddress getAddress() {
            return address;
        }

        public LocalDatagramChannel getChannel() {
            return channel;
        }
    }

    private static final class UnregisterCommand {

        private InetSocketAddress address;

        public UnregisterCommand(InetSocketAddress address) {
            Validate.notNull(address);

            this.address = address;
        }

        public InetSocketAddress getAddress() {
            return address;
        }
    }

    private static final class SendCommand {

        private InetSocketAddress from;
        private InetSocketAddress to;
        private ByteBuffer data;

        public SendCommand(InetSocketAddress from, InetSocketAddress to, ByteBuffer data) {
            Validate.notNull(from);
            Validate.notNull(to);
            Validate.notNull(data);

            this.from = from;
            this.to = to;
            this.data = data;
        }

        public InetSocketAddress getFrom() {
            return from;
        }

        public InetSocketAddress getTo() {
            return to;
        }

        public ByteBuffer getData() {
            return data;
        }

    }
    
    private static final class ShutdownCommand {
        
    }
}
