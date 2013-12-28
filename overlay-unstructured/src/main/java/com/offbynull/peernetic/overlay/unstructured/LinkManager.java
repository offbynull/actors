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
package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.Rpc;
import com.offbynull.peernetic.rpc.invoke.AsyncResultAdapter;
import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.Validate;

/**
 * Manages incoming and outgoing links. This class is thread-safe.
 * @author Kasra Faghihi
 * @param <A> address type
 */
final class LinkManager<A> {
    private Random random;
    private LinkedHashSet<A> addressCache;
    private IncomingLinkManager<A> incomingLinkManager;
    private OutgoingLinkManager<A> outgoingLinkManager;
    private LinkManagerListener<A> listener;
    private long cycleDuration;
    private int maxOutgoingLinkAttemptsPerCycle;
    private Rpc<A> rpc;
    private Lock lock;
    
    /**
     * Constructs a {@link LinkManager}.
     * @param rpc RPC object
     * @param random used to generate secrets
     * @param listener notified of link events
     * @param maxIncomingLinks maximum number of incoming links allowed
     * @param maxOutgoingLinks maximum number of outgoing links allowed
     * @param incomingLinkExpireDuration amount of time to wait before expiring an incoming link
     * @param outgoingLinkStaleDuration amount of time to wait before attempting to update an outgoing link
     * @param outgoingLinkExpireDuration amount of time to wait before expiring an outgoing link
     * @param cycleDuration amount of time to wait before attempting to make new outgoing links
     * @param maxOutgoingLinkAttemptsPerCycle maximum number of link attempts per cycle
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if any numeric argument is {@code < 0}, or if
     * {@code outgoingLinkExpireDuration <= outgoingLinkStaleDuration}, or if
     * {@code maxOutgoingLinkAttemptsPerCycle > maxOutgoingLinks}
     */
    public LinkManager(Rpc<A> rpc, Random random, LinkManagerListener<A> listener, int maxIncomingLinks, int maxOutgoingLinks,
            long incomingLinkExpireDuration, long outgoingLinkStaleDuration, long outgoingLinkExpireDuration, long cycleDuration,
            int maxOutgoingLinkAttemptsPerCycle) {
        Validate.notNull(rpc);
        Validate.notNull(random);
        Validate.notNull(listener);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxIncomingLinks);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxOutgoingLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, incomingLinkExpireDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, outgoingLinkStaleDuration);
        Validate.isTrue(outgoingLinkExpireDuration > outgoingLinkStaleDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, cycleDuration);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxOutgoingLinkAttemptsPerCycle);
        Validate.isTrue(maxOutgoingLinkAttemptsPerCycle <= maxOutgoingLinks);
        
        addressCache = new LinkedHashSet<>();
        incomingLinkManager = new IncomingLinkManager(maxIncomingLinks, incomingLinkExpireDuration);
        outgoingLinkManager = new OutgoingLinkManager(maxOutgoingLinks, outgoingLinkStaleDuration, outgoingLinkExpireDuration);
        this.rpc = rpc;
        this.random = random;
        this.listener = listener;
        this.cycleDuration = cycleDuration;
        this.maxOutgoingLinkAttemptsPerCycle = maxOutgoingLinkAttemptsPerCycle;
        lock = new ReentrantLock();
    }

    /**
     * Add addresses to address cache. Addresses used for creating outgoing links are obtained through the address cache.
     * @param addresses addresses to add
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void addToAddressCache(A ... addresses) {
        Validate.noNullElements(addresses);
        
        lock.lock();
        try {
            addressCache.addAll(Arrays.asList(addresses));
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Signals that an incoming link should be updated.
     * @param timestamp current time
     * @param address address link is from
     * @param secret secret value associated with the incoming link
     * @return {@code true} if the incoming link was updated, or {@code false} if the incoming link doesn't exist
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean updateIncomingLink(long timestamp, A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        lock.lock();
        try {
            return incomingLinkManager.updateLink(timestamp, address, secret);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Adds an incoming link.
     * @param timestamp current time
     * @param address address link is from
     * @param secret secret value associated with the incoming link
     * @return {@code true} if the incoming link was added, or {@code false} if there was no room available for it
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean addIncomingLink(long timestamp, A address, ByteBuffer secret) {
        Validate.notNull(address);
        Validate.notNull(secret);
        
        lock.lock();
        try {
            addressCache.add(address);

            if (incomingLinkManager.containsLink(address)) {
                return false;
            }
            if (outgoingLinkManager.containsLink(address)) {
                return false;
            }
            
            boolean added = incomingLinkManager.addLink(timestamp, address, secret);
            if (!added) {
                return false;
            }
        } finally {
            lock.unlock();
        }
        
        try {
            listener.linkCreated(this, LinkType.INCOMING, address);
        } catch (RuntimeException re) { // NOPMD
            // do nothing
        }
        
        return true;
    }
    
    /**
     * Gets the current set of incoming and outgoing links.
     * @return incoming/outgoing links
     */
    public State<A> getState() {
        Set<A> incomingLinks;
        Set<A> outgoingLinks;
        int freeIncomingSlots;
        
        lock.lock();
        try {
            incomingLinks = incomingLinkManager.getLinks();
            outgoingLinks = outgoingLinkManager.getLinks();
            freeIncomingSlots = incomingLinkManager.getFreeSlots();
        } finally {
            lock.unlock();
        }
        
        return new State<>(incomingLinks, outgoingLinks, freeIncomingSlots == 0);
    }

    /**
     * Called periodically to establish new connections, update stale outgoing connections, and purge expired incoming/outgoing
     * connections.
     * @param timestamp current time
     * @return next time that this method should be called
     */
    public long process(long timestamp) {
        establishNewOutgoingLinks();
        maintainExistingOutgoingLinks(timestamp);
        purgeExpiredIncomingLinks(timestamp);
        
        return timestamp + cycleDuration;
    }

    private void purgeExpiredIncomingLinks(long timestamp) {
        Set<A> killedAddresses;
        lock.lock();
        try {
            IncomingLinkManager.ProcessResult<A> result = incomingLinkManager.process(timestamp);
            killedAddresses = result.getKilledAddresses();
        } finally {
            lock.unlock();
        }
        
        for (A address : killedAddresses) {
            try {
                listener.linkDestroyed(this, LinkType.INCOMING, address);
            } catch (RuntimeException re) { //NOPMD
                // do nothing
            }
        }
    }

    private void maintainExistingOutgoingLinks(long timestamp) {
        Map<A, ByteBuffer> needsUpdateMap;
        Set<A> killedSet;
        lock.lock();
        try {
            OutgoingLinkManager.ProcessResult<A> result = outgoingLinkManager.process(timestamp);
            needsUpdateMap = result.getStaleAddresses();
            killedSet = result.getKilledAddresses();
        } finally {
            lock.unlock();
        }
        
        for (A address : killedSet) {
            try {
                listener.linkDestroyed(this, LinkType.OUTGOING, address);
            } catch (RuntimeException re) { //NOPMD
                // do nothing
            }
        }
        
        for (Entry<A, ByteBuffer> entry : needsUpdateMap.entrySet()) {
            UnstructuredServiceAsync<A> service = rpc.accessService(entry.getKey(), UnstructuredService.SERVICE_ID,
                    UnstructuredService.class, UnstructuredServiceAsync.class);  
            ByteBuffer secret = entry.getValue();
            byte[] secretData = new byte[secret.remaining()];
            secret.get(secretData);
            service.keepAlive(new KeepAliveListener(entry.getKey()), secretData);
        }
    }
    
    private void establishNewOutgoingLinks() {
        Set<A> addressesToTry = new HashSet<>();

        boolean signalEmpty = false;
        
        lock.lock();
        try {
            int remainingInOutgoingLinkManager = outgoingLinkManager.getRemaining();
            if (remainingInOutgoingLinkManager == 0) {
                return;
            }
            
            int availableInAddressCache = addressCache.size();
            signalEmpty = availableInAddressCache == 0;
            
            int numOfPossibleRequests = Math.min(availableInAddressCache, remainingInOutgoingLinkManager);
            int cappedNumOfPossibleRequests = Math.min(maxOutgoingLinkAttemptsPerCycle, numOfPossibleRequests);
            
            Iterator<A> addressCacheIt = addressCache.iterator();
            for (int i = 0; i < cappedNumOfPossibleRequests; i++) {
                A address = addressCacheIt.next();
                
                if (!incomingLinkManager.containsLink(address) && !outgoingLinkManager.containsLink(address)) {
                    System.out.println("Trying " + address + " in:" + incomingLinkManager.getLinks() + " out:"
                            + outgoingLinkManager.getLinks());
                    
                    addressesToTry.add(address);
                }
                addressCacheIt.remove();
            }
        } finally {
            lock.unlock();
        }
        
        if (signalEmpty) {
            listener.addressCacheEmpty(this);
            return;
        }
        
        for (A address : addressesToTry) {
            UnstructuredServiceAsync<A> service = rpc.accessService(address, UnstructuredService.SERVICE_ID, UnstructuredService.class,
                    UnstructuredServiceAsync.class);            
            service.getState(new GetStateResultListener(address));
        }
    }
    
    private final class KeepAliveListener implements AsyncResultListener<Boolean> {
        private A address;

        public KeepAliveListener(A address) {
            this.address = address;
        }

        @Override
        public void invokationReturned(Boolean object) {
            Validate.notNull(object);
            
            if (!object) {
                lock.lock();
                try {
                    outgoingLinkManager.removeLink(address);
                } finally {
                    lock.unlock();
                }

                try {
                    listener.linkDestroyed(LinkManager.this, LinkType.OUTGOING, address);
                } catch (RuntimeException re) { // NOPMD
                    // do nothing
                }
            } else {
                lock.lock();
                try {
                    boolean updated = outgoingLinkManager.updateLink(System.currentTimeMillis(), address);
                    if (!updated) {
                        System.out.println("update failed for " + address);
                        try {
                            listener.linkDestroyed(LinkManager.this, LinkType.OUTGOING, address);
                        } catch (RuntimeException re) { // NOPMD
                            // do nothing
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public void invokationThrew(Throwable err) {
            lock.lock();
            try {
                outgoingLinkManager.removeLink(address);
            } finally {
                lock.unlock();
            }
            
            if (listener != null) {
                try {
                    listener.linkDestroyed(LinkManager.this, LinkType.OUTGOING, address);
                } catch (RuntimeException re) { // NOPMD
                    // do nothing
                }
            }
        }

        @Override
        public void invokationFailed(Object err) {
            lock.lock();
            try {
                outgoingLinkManager.removeLink(address);
            } finally {
                lock.unlock();
            }
            
            try {
                listener.linkDestroyed(LinkManager.this, LinkType.OUTGOING, address);
            } catch (RuntimeException re) { // NOPMD
                // do nothing
            }
        }
    }
    
    private final class GetStateResultListener extends AsyncResultAdapter<State<A>> {
        private A address;

        public GetStateResultListener(A address) {
            this.address = address;
        }

        @Override
        public void invokationReturned(State<A> object) {
            Validate.notNull(object);
            Validate.noNullElements(object.getIncomingLinks());
            Validate.noNullElements(object.getOutgoingLinks());
            
            lock.lock();
            try {
                addressCache.addAll(object.getIncomingLinks());
                addressCache.addAll(object.getOutgoingLinks());
            } finally {
                lock.unlock();
            }
            
            if (object.isIncomingLinksFull()) {
                return;
            }

            UnstructuredServiceAsync<A> service = rpc.accessService(address, UnstructuredService.SERVICE_ID, UnstructuredService.class,
                    UnstructuredServiceAsync.class);
            
            byte[] secret = new byte[UnstructuredService.SECRET_SIZE];
            random.nextBytes(secret);
            service.join(new JoinResultListener(address, ByteBuffer.wrap(secret)), secret);
        }
    }

    private final class JoinResultListener extends AsyncResultAdapter<Boolean> {
        private A address;
        private ByteBuffer secret;

        public JoinResultListener(A address, ByteBuffer secret) {
            this.address = address;
            this.secret = secret;
        }

        @Override
        public void invokationReturned(Boolean object) {
            Validate.notNull(object);
            
            if (object) {
                lock.lock();
                try {
                    if (incomingLinkManager.containsLink(address)) {
                        return;
                    }
                    if (outgoingLinkManager.containsLink(address)) {
                        return;
                    }

                    boolean added = outgoingLinkManager.addLink(System.currentTimeMillis(), address, secret);
                    if (!added) {
                        return;
                    }
                    
                    addressCache.add(address);
                } finally {
                    lock.unlock();
                }
                
                try {
                    listener.linkCreated(LinkManager.this, LinkType.OUTGOING, address);
                } catch (RuntimeException re) { // NOPMD
                    // do nothing
                }
            }
        }
    }
}
