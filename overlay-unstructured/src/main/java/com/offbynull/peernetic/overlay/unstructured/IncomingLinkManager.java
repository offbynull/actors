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
import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import org.apache.commons.lang3.Validate;

final class IncomingLinkManager<A> {
    private int maxLinks;
    
    private TimeoutManager<Endpoint> expireTimeoutManager;
    private long expireDuration;
    
    private LinkRepository<A> linkRepository;

    public IncomingLinkManager(int maxLinks, long expireDuration, LinkRepository<A> linkRepository) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, expireDuration);
        Validate.notNull(linkRepository);
        
        this.maxLinks = maxLinks;
        this.expireDuration = expireDuration;
        expireTimeoutManager = new TimeoutManager<>();
        
        this.linkRepository = linkRepository;
    }

    public boolean processCommand(long timestamp, Endpoint from, Object content, PushQueue pushQueue) {
        if (content instanceof InitiateJoinCommand) {
            //InitiateJoinCommand ijc = (InitiateJoinCommand) content;
            
            if (expireTimeoutManager.contains(from)) {
                pushQueue.push(from, new JoinSuccessfulCommand(linkRepository.toState()));
                return true;
            }
            
            if (expireTimeoutManager.size() == maxLinks
                    || linkRepository.containsLink(LinkType.OUTGOING, from)
                    || linkRepository.containsLink(LinkType.INCOMING, from)) { // last check added just incase
                pushQueue.push(from, new JoinFailedCommand(linkRepository.toState()));
                return true;
            }
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            linkRepository.addLink(LinkType.INCOMING, from);
            
            State<A> state = linkRepository.toState();
            pushQueue.push(from, new JoinSuccessfulCommand(state));
            
            return true;
        } else if (content instanceof InitiateKeepAliveCommand) {
            //InitiateKeepAliveCommand ikac = (InitiateKeepAliveCommand) content;
            
            if (!expireTimeoutManager.contains(from)) {
                expireTimeoutManager.cancel(from);
                linkRepository.removeLink(LinkType.INCOMING, from);
                pushQueue.push(from, new KeepAliveFailedCommand(linkRepository.toState()));
                return true;
            }
            
            expireTimeoutManager.cancel(from);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            State<A> state = linkRepository.toState();
            pushQueue.push(from, new KeepAliveSuccessfulCommand(state));
            
            return true;
        }
        
        return false;
    }
    
    public long process(long timestamp, PushQueue pushQueue) {
        TimeoutManager.TimeoutManagerResult<Endpoint> expiredTimeouts = expireTimeoutManager.process(timestamp);
        for (Endpoint endpoint : expiredTimeouts.getTimedout()) {
            linkRepository.removeLink(LinkType.INCOMING, endpoint);
        }
        
        return expiredTimeouts.getNextTimeoutTimestamp();
    }
}
