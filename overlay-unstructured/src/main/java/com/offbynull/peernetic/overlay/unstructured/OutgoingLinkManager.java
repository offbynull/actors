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

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.actor.helpers.TimeoutManager.TimeoutManagerResult;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkManager<A> {
    private int maxLinks;
    
    private Map<Endpoint, ByteBuffer> links;
    private TimeoutManager<Endpoint> staleTimeoutManager;
    private TimeoutManager<Endpoint> expireTimeoutManager;
    private long staleDuration;
    private long expireDuration;
    
    private Map<Endpoint, ByteBuffer> pendingLinks;
    private TimeoutManager<Endpoint> pendingTimeoutManager;
    private long pendingDuration;
    
    private LinkRepository<A> linkRepository;
    
    private SecureRandom random;
    

    public OutgoingLinkManager(LinkRepository<A> linkRepository, int maxLinks, long staleDuration, long expireDuration,
            long pendingDuration) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, pendingDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, staleDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, expireDuration);
        Validate.isTrue(staleDuration < expireDuration);
        Validate.notNull(linkRepository);
        
        this.maxLinks = maxLinks;
        
        links = new HashMap<>();
        this.staleDuration = staleDuration;
        this.expireDuration = expireDuration;
        staleTimeoutManager = new TimeoutManager<>();
        expireTimeoutManager = new TimeoutManager<>();

        pendingLinks = new HashMap<>();
        this.pendingDuration = pendingDuration;
        pendingTimeoutManager = new TimeoutManager<>();
        
        this.linkRepository = linkRepository;
        
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae);
        }
    }
    
    public boolean processCommand(long timestamp, Endpoint from, Object content, PushQueue pushQueue) {
        if (content instanceof JoinSuccessfulCommand) {
            pendingTimeoutManager.cancel(from);
            ByteBuffer secret = pendingLinks.remove(from);
            
            if (secret == null) {
                return true;
            }

            links.put(from, secret);
            staleTimeoutManager.add(from, timestamp + staleDuration);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            linkRepository.addLink(LinkType.OUTGOING, from);
            return true;
        } else if (content instanceof JoinFailedCommand) {
            pendingTimeoutManager.cancel(from);
            pendingLinks.remove(from);
            return true;
        } else if (content instanceof KeepAliveSuccessfulCommand) {
            ByteBuffer secret = links.remove(from);
            
            if (secret == null) {
                return true;
            }
            
            staleTimeoutManager.cancel(from);
            expireTimeoutManager.cancel(from);
            
            links.put(from, secret);
            staleTimeoutManager.add(from, timestamp + staleDuration);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            return true;
        } else if (content instanceof KeepAliveFailedCommand) {
            links.remove(from);
            staleTimeoutManager.cancel(from);
            expireTimeoutManager.cancel(from);
            
            linkRepository.removeLink(LinkType.OUTGOING, from);
            
            return true;
        }
        
        return false;
    }
    
    public long process(long timestamp, PushQueue pushQueue) {
        TimeoutManagerResult<Endpoint> pendingTimeouts = pendingTimeoutManager.process(timestamp);
        for (Endpoint endpoint : pendingTimeouts.getTimedout()) {
            pendingLinks.remove(endpoint);
        } 
        
        TimeoutManagerResult<Endpoint> staleTimeouts = staleTimeoutManager.process(timestamp);
        for (Endpoint endpoint : staleTimeouts.getTimedout()) {
            ByteBuffer secret = links.get(endpoint);
            
            pushQueue.push(endpoint, new InitiateKeepAliveCommand(secret));
            staleTimeoutManager.add(endpoint, timestamp + staleDuration);
        }
        
        TimeoutManagerResult<Endpoint> expiredTimeouts = expireTimeoutManager.process(timestamp);
        for (Endpoint endpoint : expiredTimeouts.getTimedout()) {
            links.remove(endpoint);
            staleTimeoutManager.cancel(endpoint);
            linkRepository.removeLink(LinkType.OUTGOING, endpoint);
            //expireTimeoutManager.cancel(endpoint); // already taken out by call to process above
        }
        
        initiateOutgoingLink(timestamp, pushQueue);
        
        long waitUntil = Long.MAX_VALUE;
        waitUntil = Math.min(waitUntil, pendingTimeouts.getNextTimeoutTimestamp());
        waitUntil = Math.min(waitUntil, staleTimeouts.getNextTimeoutTimestamp());
        waitUntil = Math.min(waitUntil, expiredTimeouts.getNextTimeoutTimestamp());
        
        return waitUntil;
    }
    
    private boolean initiateOutgoingLink(long timestamp, PushQueue pushQueue) {
        if (pendingLinks.size() == 5) { // make this into a configurable param later
            return false;
        }
        
        int currentLinks = pendingLinks.size() + links.size();
        if (currentLinks >= maxLinks) {
            return false;
        }
        
        Endpoint endpoint = linkRepository.peekNextCache();
        if (endpoint == null || pendingLinks.containsKey(endpoint) || links.containsKey(endpoint)) {
            return false;
        }
        
        linkRepository.pollNextCache(); // remove

        ByteBuffer secret = ByteBuffer.allocate(Constants.SECRET_SIZE);
        random.nextBytes(secret.array());
        secret = secret.asReadOnlyBuffer();

        pendingTimeoutManager.add(endpoint, timestamp + pendingDuration);
        pendingLinks.put(endpoint, secret);
        pushQueue.push(endpoint, new InitiateJoinCommand(secret));
        
        return true;
    }
}
