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

import com.offbynull.peernetic.common.concurrent.actor.Actor;
import com.offbynull.peernetic.common.concurrent.actor.ActorQueue;
import com.offbynull.peernetic.common.concurrent.actor.Message;
import com.offbynull.peernetic.common.concurrent.actor.PushQueue;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager.TimeoutManagerResult;
import com.offbynull.peernetic.rpc.Rpc;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * An unstructured overlay service.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class UnstructuredOverlay<A> extends Actor {
    
    private Rpc<A> rpc;
    private UnstructuredServiceImplementation<A> unstructuredService;
    private UnstructuredOverlayListener<A> listener;
    
    private LinkedHashSet<A> addressCache;
    private Map<A, ByteBuffer> incomingLinks; // address to secret
    private TimeoutManager<A> incomingLinkExpireTimeoutManager;
    private Map<A, ByteBuffer> outgoingLinks; // address to secret
    private TimeoutManager<A> outgoingLinkStaleTimeoutManager;
    private TimeoutManager<A> outgoingLinkExpireTimeoutManager;
    private TimeoutManager<Object> addressCacheNotifyManager;
    private int maxIncomingLinks;
    private int maxOutgoingLinks;
    private int currentJoinAttempts;
    private int maxJoinAttemptsAtOnce;
    private long outgoingStaleDuration;
    private long outgoingExpireDuration;
    private long incomingExpireDuration;
    private long addressCacheEmptyNotifyDuration;
    private SecureRandom random;
    
    /**
     * Constructs a {@link UnstructuredOverlay} object with default configurations. Equivalent to calling
     * {@code UnstructuredOverlay(rpc, listener, new UnstructuredOverlayConfig())}.
     * @param rpc RPC
     * @param listener listener
     * @param maxIncomingLinks maximum number of incoming links allowed
     * @param maxOutgoingLinks maximum number of outgoing links allowed
     * @param maxJoinAttemptsAtOnce maximum number of outgoing links that can be initiated at the same time
     * @param outgoingStaleDuration maximum amount of time to wait before attempting a keepalive on an outgoing connection
     * @param outgoingExpireDuration maximum amount of time to wait for a sent keepalive to be validated for an outgoing link before
     * dropping it
     * @param incomingExpireDuration maximum amount of time to wait for a keepalive to come in for an incoming link before dropping it
     * @param addressCacheEmptyNotifyDuration maximum amount of time before attempting new outgoing connections
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if any numeric argument is negative
     */
    public UnstructuredOverlay(Rpc<A> rpc, UnstructuredOverlayListener<A> listener, int maxIncomingLinks, int maxOutgoingLinks,
            int maxJoinAttemptsAtOnce, long outgoingStaleDuration, long outgoingExpireDuration, long incomingExpireDuration,
            long addressCacheEmptyNotifyDuration) {
        super(true);
        
        Validate.notNull(rpc);
        Validate.notNull(listener);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxIncomingLinks);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxOutgoingLinks);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxJoinAttemptsAtOnce);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, outgoingStaleDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, outgoingExpireDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, incomingExpireDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, addressCacheEmptyNotifyDuration);
        
        this.addressCache = new LinkedHashSet<>();
        this.unstructuredService = new UnstructuredServiceImplementation(getSelfWriter());
        this.rpc = rpc;
        this.maxIncomingLinks = maxIncomingLinks;
        this.maxOutgoingLinks = maxOutgoingLinks;
        this.maxJoinAttemptsAtOnce = maxJoinAttemptsAtOnce;
        this.outgoingStaleDuration = outgoingStaleDuration;
        this.outgoingExpireDuration = outgoingExpireDuration;
        this.incomingExpireDuration = incomingExpireDuration;
        this.addressCacheEmptyNotifyDuration = addressCacheEmptyNotifyDuration;
        this.listener = listener;
    }

    @Override
    protected ActorQueue createQueue() {
        return new ActorQueue();
    }

    @Override
    protected void onStart() throws Exception {
        incomingLinks = new HashMap<>();
        incomingLinkExpireTimeoutManager = new TimeoutManager<>();
        outgoingLinks = new HashMap<>();
        outgoingLinkStaleTimeoutManager = new TimeoutManager<>();
        outgoingLinkExpireTimeoutManager = new TimeoutManager<>();
        addressCacheNotifyManager = new TimeoutManager<>();
        random = SecureRandom.getInstance("SHA1PRNG");

        rpc.addService(UnstructuredServiceImplementation.SERVICE_ID, unstructuredService);
    }
    
    @Override
    protected long onStep(long timestamp, Iterator<Message> iterator, PushQueue pushQueue) throws Exception {
        while (iterator.hasNext()) {
            Message msg = iterator.next();
            Object content = msg.getContent();
            
            if (content instanceof GetStateCommand) {
                GetStateCommand<A> gsc = (GetStateCommand<A>) content;
                CommandResponseListener<State<A>> callback = gsc.getCallback();
                
                State<A> state = new State<>(incomingLinks.keySet(), outgoingLinks.keySet(), incomingLinks.size() == maxIncomingLinks);
                callback.commandResponded(state);
            } else if (content instanceof AddToAddressCacheCommand) {
                AddToAddressCacheCommand<A> atacc = (AddToAddressCacheCommand<A>) content;
                addressCache.addAll(atacc.getAddresses());
            } else if (content instanceof InitiateJoinCommand) {
                InitiateJoinCommand<A> jc = (InitiateJoinCommand<A>) content;
                CommandResponseListener<Boolean> callback = jc.getCallback();
                
                if (incomingLinks.size() == maxIncomingLinks) {
                    callback.commandResponded(false);
                    continue;
                } else {
                    callback.commandResponded(true);
                }
                
                A address = jc.getAddress();
                ByteBuffer secret = jc.getSecret();

                incomingLinks.put(address, secret);
                incomingLinkExpireTimeoutManager.add(address, timestamp + incomingExpireDuration);
                
                listener.linkCreated(this, LinkType.INCOMING, address);
            } else if (content instanceof InitiateKeepAliveCommand) {
                InitiateKeepAliveCommand<A> kac = (InitiateKeepAliveCommand<A>) content;
                CommandResponseListener<Boolean> callback = kac.getCallback();
                
                A address = kac.getAddress();
                ByteBuffer secret = kac.getSecret();

                if (!secret.equals(incomingLinks.get(address))) {
                    callback.commandResponded(false);
                } else {
                    callback.commandResponded(true);
                }
                
                incomingLinkExpireTimeoutManager.cancel(address);
                incomingLinkExpireTimeoutManager.add(address, timestamp + incomingExpireDuration);
            } else if (content instanceof JoinSuccessfulCommand) {
                JoinSuccessfulCommand<A> jsc = (JoinSuccessfulCommand<A>) content;
                currentJoinAttempts--;
                
                A address = jsc.getAddress();
                ByteBuffer secret = jsc.getSecret();
                
                outgoingLinks.put(jsc.getAddress(), secret);
                outgoingLinkStaleTimeoutManager.add(address, timestamp + outgoingStaleDuration);

                listener.linkCreated(this, LinkType.OUTGOING, address);
            } else if (content instanceof JoinFailedCommand) {
                //JoinFailedCommand<A> jfc = (JoinFailedCommand<A>) content;
                currentJoinAttempts--;
            } else if (content instanceof KeepAliveSuccessfulCommand) {
                KeepAliveSuccessfulCommand<A> kasc = (KeepAliveSuccessfulCommand<A>) content;
                
                A address = kasc.getAddress();
                
                if (!outgoingLinks.containsKey(address)) {
                    continue;
                }

                outgoingLinkStaleTimeoutManager.cancel(address);
                outgoingLinkExpireTimeoutManager.cancel(address);
                
                outgoingLinkStaleTimeoutManager.add(address, timestamp + outgoingStaleDuration);
            } else if (content instanceof KeepAliveFailedCommand) {
                KeepAliveFailedCommand<A> kafc = (KeepAliveFailedCommand<A>) content;
                A address = kafc.getAddress();
                
                outgoingLinks.remove(kafc.getAddress());
                outgoingLinkStaleTimeoutManager.cancel(address);
                outgoingLinkExpireTimeoutManager.cancel(address);
                
                listener.linkDestroyed(this, LinkType.OUTGOING, address);
            } else {
                throw new IllegalArgumentException();
            }
        }
        
        TimeoutManagerResult<A> expiredIncoming = incomingLinkExpireTimeoutManager.process(timestamp);
        for (A address : expiredIncoming.getTimedout()) {
            incomingLinks.remove(address);
            listener.linkDestroyed(this, LinkType.INCOMING, address);
        }
        
        TimeoutManagerResult<A> staleOutgoing = outgoingLinkStaleTimeoutManager.process(timestamp);
        for (A address : staleOutgoing.getTimedout()) {
            outgoingLinkExpireTimeoutManager.add(address, timestamp + outgoingExpireDuration);
            InternalUnstructuredOverlayUtils.invokeKeepAlive(getSelfWriter(), rpc, address, outgoingLinks.get(address).asReadOnlyBuffer());
        }
        
        TimeoutManagerResult<A> expiredOutgoing = outgoingLinkExpireTimeoutManager.process(timestamp);
        for (A address : expiredOutgoing.getTimedout()) {
            outgoingLinks.remove(address);
            listener.linkDestroyed(this, LinkType.OUTGOING, address);
        }
        
        TimeoutManagerResult<Object> addressCacheNotify = addressCacheNotifyManager.process(timestamp);
        if (!addressCacheNotify.getTimedout().isEmpty()) { //NOPMD
            //listener.addressCacheEmpty(this);
        }
        
        if (addressCacheNotifyManager.isEmpty()) {
            addressCacheNotifyManager.add(new Object(), timestamp + addressCacheEmptyNotifyDuration);
        }
        
        while (outgoingLinks.size() != maxOutgoingLinks && !addressCache.isEmpty() && currentJoinAttempts < maxJoinAttemptsAtOnce) {
            Iterator<A> it = addressCache.iterator();
            A address = it.next();
            it.remove();
            
            ByteBuffer secret = ByteBuffer.allocate(UnstructuredService.SECRET_SIZE);
            random.nextBytes(secret.array());
            secret = secret.asReadOnlyBuffer();
            
            InternalUnstructuredOverlayUtils.invokeJoin(getSelfWriter(), rpc, address, secret.asReadOnlyBuffer());
            currentJoinAttempts++;
        }
        
        long waitDuration = Long.MAX_VALUE;
        waitDuration = Math.min(waitDuration, expiredIncoming.getNextTimeoutTimestamp());
        waitDuration = Math.min(waitDuration, staleOutgoing.getNextTimeoutTimestamp());
        waitDuration = Math.min(waitDuration, expiredOutgoing.getNextTimeoutTimestamp());
        
        return waitDuration;
    }
    
    @Override
    protected void onStop(PushQueue pushQueue) throws Exception {
        rpc.removeService(UnstructuredServiceImplementation.SERVICE_ID);
    }
    
    /**
     * Queue new addresses to go in the address cache.
     * @param addresses new addresses to go in the address cache
     */
    public void addToAddressCache(A ... addresses) {
        getSelfWriter().push(Message.createOneWayMessage(new AddToAddressCacheCommand<>(new LinkedHashSet<>(Arrays.asList(addresses)))));
    }
}
