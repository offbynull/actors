/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.core.gateway.servlet;

import com.offbynull.actors.core.shuttle.Message;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Supplier;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.Validate;

/**
 * An in-memory message cache that clears buffered messages for IDs after some amount of inactivity.
 * @author Kasra Faghihi
 */
public final class InMemoryMessageCache implements MessageCache {

    private final Map<String, ClientDataState> dataStates;
    private final BidiMap<String, ClientLastAccessState> lastAccessStates;
    private final PriorityQueue<ClientLastAccessState> timeoutQueue;
    private final long timeout;
    
    private final Supplier<Long> timeSupplier;
    
    private final Object lock = new Object();

    /**
     * Constructs a {@link InMemoryMessageCache} object.
     * @param timeout client timeout
     * @throws IllegalArgumentException if {@code timeout <= 0L}
     */
    public InMemoryMessageCache(long timeout) {
        this(
                timeout,
                new Supplier<Long>() {
                    private long lastTime = Long.MIN_VALUE;

                    @Override
                    public Long get() {
                        // http://stackoverflow.com/questions/2978598/will-system-currenttimemillis-always-return-a-value-previous-calls
                        //
                        // On some machines, currentTimeMillis() is not monotonic (it may return a value lower than what it previously
                        // returned). As such, we don't return values that are less.
                        long time = System.currentTimeMillis();
                        return time < lastTime ? lastTime : time;
                    }
                }
        );
    }

    // Use this constructor directly for unit testing
    InMemoryMessageCache(long timeout, Supplier<Long> timeSupplier) {
        Validate.isTrue(timeout > 0L);
        Validate.notNull(timeSupplier);

        this.dataStates = new HashMap<>();
        this.lastAccessStates = new DualHashBidiMap<>();
        this.timeoutQueue = new PriorityQueue<>();
        this.timeout = timeout;
        
        this.timeSupplier = timeSupplier;
    }

    @Override
    public void keepAlive(String id) {
        Validate.notNull(id);

        synchronized (lock) {
            long time = timeSupplier.get();
            
            ClientLastAccessState newLastAccessState = new ClientLastAccessState(time);
            ClientLastAccessState oldLastAccessState = lastAccessStates.put(id, newLastAccessState);
            timeoutQueue.add(newLastAccessState);
            if (oldLastAccessState != null) {
                oldLastAccessState.ignore = true;
            }

            dataStates.computeIfAbsent(id, x -> new ClientDataState());
        }
    }

    @Override
    public void systemToHttpAppend(String id, List<Message> messages) {
        Validate.notNull(id);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        synchronized (lock) {
            long time = timeSupplier.get();

            clear(time);

            ClientDataState clientDataState = dataStates.get(id);
            Validate.isTrue(clientDataState != null, "ID not tracked");

            int seqOffset = clientDataState.getOutgoingSequenceOffset();
            for (Message message : messages) {
                clientDataState.addOutgoing(seqOffset, message);
                seqOffset = Math.addExact(seqOffset, 1);
            }
        }
    }

    @Override
    public void systemToHttpAcknowledge(String id, int maxSeqOffset) {
        Validate.notNull(id);
        Validate.notNull(maxSeqOffset >= 0);
        
        synchronized (lock) {
            long time = timeSupplier.get();

            clear(time);

            ClientDataState clientDataState = dataStates.get(id);
            Validate.isTrue(clientDataState != null, "ID not tracked");

            clientDataState.acknowledgeOutgoing(maxSeqOffset);
        }
    }

    @Override
    public MessageBlock systemToHttpRead(String id) {
        Validate.notNull(id);

        synchronized (lock) {
            long time = timeSupplier.get();

            clear(time);

            ClientDataState clientDataState = dataStates.get(id);
            Validate.isTrue(clientDataState != null, "ID not tracked");

            return clientDataState.getOutgoing();
        }
    }

    @Override
    public void httpToSystemAdd(String id, int seqOffset, List<Message> messages) {
        Validate.notNull(id);
        Validate.isTrue(seqOffset >= 0);
        Validate.notNull(messages);
        Validate.noNullElements(messages);

        synchronized (lock) {
            long time = timeSupplier.get();

            clear(time);

            ClientDataState clientDataState = dataStates.get(id);
            Validate.isTrue(clientDataState != null, "ID not tracked");

            for (Message message : messages) {
                clientDataState.addIncoming(seqOffset, message);
                seqOffset = Math.addExact(seqOffset, 1);
            }
        }
    }

    @Override
    public void httpToSystemClear(String id) {
        Validate.notNull(id);

        synchronized (lock) {
            long time = timeSupplier.get();

            clear(time);

            ClientDataState clientDataState = dataStates.get(id);
            Validate.isTrue(clientDataState != null, "ID not tracked");

            clientDataState.clearIncoming();
        }
    }

    @Override
    public MessageBlock httpToSystemRead(String id) {
        Validate.notNull(id);

        synchronized (lock) {
            long time = timeSupplier.get();

            clear(time);

            ClientDataState clientDataState = dataStates.get(id);
            Validate.isTrue(clientDataState != null, "ID not tracked");

            return clientDataState.getOutgoing();
        }
    }

    private void clear(long time) {
        ClientLastAccessState entry;
        while ((entry = timeoutQueue.peek()) != null) {
            if (entry.ignore) {
                timeoutQueue.poll();
                continue;
            }

            long diff = time - entry.lastAccessTime;
            if (diff >= timeout) {
                timeoutQueue.poll();
                String id = lastAccessStates.removeValue(entry);
                if (id != null) {
                    dataStates.remove(id);
                }
                continue;
            }

            break;
        }
    }

    private static final class ClientLastAccessState implements Comparator<ClientLastAccessState> {

        private final long lastAccessTime;
        private boolean ignore;

        public ClientLastAccessState(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        @Override
        public int compare(ClientLastAccessState o1, ClientLastAccessState o2) {
            return Long.compare(o1.lastAccessTime, o2.lastAccessTime);
        }
    }

    private static final class ClientDataState {

        private int incomingSequenceOffset;
        private final LinkedList<Message> incomingMessages;
        private int outgoingSequenceOffset;
        private final LinkedList<Message> outgoingMessages;

        public ClientDataState() {
            outgoingSequenceOffset = -1;
            incomingMessages = new LinkedList<>();
            incomingSequenceOffset = -1;
            outgoingMessages = new LinkedList<>();
        }

        public void addIncoming(int seq, Message message) {
            Validate.isTrue(seq >= 0);
            int nextOffset = Math.addExact(incomingSequenceOffset, 1);
            Validate.isTrue(seq == nextOffset);
            Validate.notNull(message);

            incomingMessages.add(message);
            incomingSequenceOffset = nextOffset;
        }

        public void clearIncoming() {
            incomingMessages.clear();
        }

        public MessageBlock getIncoming() {
            return new MessageBlock(incomingSequenceOffset, incomingMessages);
        }

        public int getOutgoingSequenceOffset() {
            return outgoingSequenceOffset;
        }

        public void addOutgoing(int seq, Message message) {
            Validate.isTrue(seq >= 0);
            int nextOffset = Math.addExact(outgoingSequenceOffset, 1);
            Validate.isTrue(seq == nextOffset);
            Validate.notNull(message);

            outgoingMessages.add(message);
            outgoingSequenceOffset = nextOffset;
        }

        public void acknowledgeOutgoing(int seq) {
            Validate.isTrue(seq >= 0);
            long seqEndOffset = Math.addExact(outgoingSequenceOffset, outgoingMessages.size()) - 1;
            Validate.isTrue(seq >= 0L && seq <= seqEndOffset);

            while (outgoingSequenceOffset < seq) {
                outgoingMessages.pop();
                outgoingSequenceOffset = Math.addExact(outgoingSequenceOffset, 1);
            }
        }

        public MessageBlock getOutgoing() {
            return new MessageBlock(outgoingSequenceOffset, outgoingMessages);
        }
    }
}
