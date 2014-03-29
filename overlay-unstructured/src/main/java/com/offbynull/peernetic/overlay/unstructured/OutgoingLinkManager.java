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
package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.NotifyManager;
import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import com.offbynull.peernetic.actor.helpers.TimeoutManager.TimeoutManagerResult;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkManager<A> {
    private int maxLinks;
    
    private TimeoutManager<Endpoint> staleTimeoutManager;
    private TimeoutManager<Endpoint> expireTimeoutManager;
    private long staleDuration;
    private long expireDuration;
    
    private TimeoutManager<Endpoint> pendingTimeoutManager;
    private long pendingDuration;
    
    private NotifyManager joinNotifyManager;
    
    private LinkRepository<A> linkRepository;
    

    public OutgoingLinkManager(LinkRepository<A> linkRepository, int maxLinks, long staleDuration, long expireDuration,
            long pendingDuration) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, pendingDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, staleDuration);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, expireDuration);
        Validate.isTrue(staleDuration < expireDuration);
        Validate.notNull(linkRepository);
        
        this.maxLinks = maxLinks;
        
        this.staleDuration = staleDuration;
        this.expireDuration = expireDuration;
        staleTimeoutManager = new TimeoutManager<>();
        expireTimeoutManager = new TimeoutManager<>();

        this.pendingDuration = pendingDuration;
        pendingTimeoutManager = new TimeoutManager<>();
        
        joinNotifyManager = new NotifyManager();
        
        this.linkRepository = linkRepository;
    }
    
    public void init(long timestamp) {
        joinNotifyManager.reset(Long.MIN_VALUE); // first hit should be as soon as possible
    }
    
    public boolean processCommand(long timestamp, Endpoint from, Object content, PushQueue pushQueue) {
        if (content instanceof JoinSuccessfulCommand) {
            JoinSuccessfulCommand jsc = (JoinSuccessfulCommand) content;
            
            boolean canceled = pendingTimeoutManager.cancel(from);
            
            if (!canceled) {
                return true;
            }

            staleTimeoutManager.add(from, timestamp + staleDuration);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            linkRepository.addLink(LinkType.OUTGOING, from);
            
            linkRepository.addStateToCache(jsc.getState());
            return true;
        } else if (content instanceof JoinFailedCommand) {
            JoinFailedCommand jfc = (JoinFailedCommand) content;
            
            pendingTimeoutManager.cancel(from);
            
            linkRepository.addStateToCache(jfc.getState());
            return true;
        } else if (content instanceof KeepAliveSuccessfulCommand) {
            KeepAliveSuccessfulCommand kasc = (KeepAliveSuccessfulCommand) content;
            
            staleTimeoutManager.cancel(from);
            boolean stillActive = expireTimeoutManager.cancel(from);
            
            if (!stillActive) {
                return true;
            }
            
            staleTimeoutManager.add(from, timestamp + staleDuration);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            linkRepository.addStateToCache(kasc.getState());
            return true;
        } else if (content instanceof KeepAliveFailedCommand) {
            KeepAliveFailedCommand kafc = (KeepAliveFailedCommand) content;

            staleTimeoutManager.cancel(from);
            expireTimeoutManager.cancel(from);
            
            linkRepository.removeLink(LinkType.OUTGOING, from);
            
            linkRepository.addStateToCache(kafc.getState());
            return true;
        }
        
        return false;
    }
    
    public long process(long timestamp, PushQueue pushQueue) {
        TimeoutManagerResult<Endpoint> pendingTimeouts = pendingTimeoutManager.process(timestamp);
        
        TimeoutManagerResult<Endpoint> expiredTimeouts = expireTimeoutManager.process(timestamp);
        for (Endpoint endpoint : expiredTimeouts.getTimedout()) {
            staleTimeoutManager.cancel(endpoint);
            linkRepository.removeLink(LinkType.OUTGOING, endpoint);
            //expireTimeoutManager.cancel(endpoint); // already taken out by call to process above
        }

        TimeoutManagerResult<Endpoint> staleTimeouts = staleTimeoutManager.process(timestamp);
        for (Endpoint endpoint : staleTimeouts.getTimedout()) {
            pushQueue.push(endpoint, new InitiateKeepAliveCommand());
            staleTimeoutManager.add(endpoint, timestamp + staleDuration);
        }
        
        long nextInitiateOutgoingInvokeTimestamp = initiateOutgoingLink(timestamp, pushQueue);
        
        long waitUntil = Long.MAX_VALUE;
        waitUntil = Math.min(waitUntil, pendingTimeouts.getNextTimeoutTimestamp());
        waitUntil = Math.min(waitUntil, staleTimeouts.getNextTimeoutTimestamp());
        waitUntil = Math.min(waitUntil, expiredTimeouts.getNextTimeoutTimestamp());
        waitUntil = Math.min(waitUntil, nextInitiateOutgoingInvokeTimestamp);
        
        return waitUntil;
    }
    
    private long initiateOutgoingLink(long timestamp, PushQueue pushQueue) {
        if (!joinNotifyManager.process(timestamp)) {
            return joinNotifyManager.getNextTimeoutTimestamp();
        }
        
        joinNotifyManager.reset(timestamp + expireDuration);

        int currentLinks = pendingTimeoutManager.size() + expireTimeoutManager.size();
        if (currentLinks >= maxLinks) {
            return Long.MAX_VALUE;
        }
        
        while (pendingTimeoutManager.size() < 5) { // make this into a configurable param later
            Endpoint endpoint = linkRepository.pollNextCache(); // remove
            
            if (endpoint == null) {
                break;
            }
            
            if (pendingTimeoutManager.contains(endpoint) || expireTimeoutManager.contains(endpoint)
                    || linkRepository.containsLink(LinkType.INCOMING, endpoint)
                    || linkRepository.containsLink(LinkType.OUTGOING, endpoint)) { // last check added just incase
                continue;
            }

            pendingTimeoutManager.add(endpoint, timestamp + pendingDuration);
            pushQueue.push(endpoint, new InitiateJoinCommand());
        }
        
        return joinNotifyManager.getNextTimeoutTimestamp();
    }
}
