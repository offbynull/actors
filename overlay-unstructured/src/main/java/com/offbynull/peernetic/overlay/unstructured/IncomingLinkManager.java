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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

final class IncomingLinkManager<A> {
    private int maxLinks;
    
    private Map<Endpoint, ByteBuffer> links; // address to secret
    private TimeoutManager<Endpoint> expireTimeoutManager;
    private long expireDuration;
    
    private LinkRepository<A> linkRepository;

    public IncomingLinkManager(int maxLinks, long expireDuration, LinkRepository<A> linkRepository) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, expireDuration);
        Validate.notNull(linkRepository);
        
        this.maxLinks = maxLinks;
        this.expireDuration = expireDuration;
        links = new HashMap<>();
        expireTimeoutManager = new TimeoutManager<>();
        
        this.linkRepository = linkRepository;
    }

    public boolean processCommand(long timestamp, Endpoint from, Object content, PushQueue pushQueue) {
        if (content instanceof InitiateJoinCommand) {
            InitiateJoinCommand ijc = (InitiateJoinCommand) content;
            
            ByteBuffer secret = ijc.getSecret();
            if (links.size() == maxLinks || links.containsKey(from) || secret.remaining() != Constants.SECRET_SIZE) {
                pushQueue.push(from, new JoinFailedCommand());
                return true;
            }
            
            links.put(from, secret);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            linkRepository.addLink(LinkType.INCOMING, from);
            
            State<A> state = linkRepository.toState();
            pushQueue.push(from, new JoinSuccessfulCommand(state));
            
            return true;
        } else if (content instanceof InitiateKeepAliveCommand) {
            InitiateKeepAliveCommand ikac = (InitiateKeepAliveCommand) content;
            
            ByteBuffer secret = ikac.getSecret();
            if (!links.containsKey(from) || secret.remaining() != Constants.SECRET_SIZE || !links.get(from).equals(secret)) {
                expireTimeoutManager.cancel(from);
                linkRepository.removeLink(LinkType.INCOMING, from);
                pushQueue.push(from, new KeepAliveFailedCommand());
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
            links.remove(endpoint);
            linkRepository.removeLink(LinkType.INCOMING, endpoint);
        }
        
        return expiredTimeouts.getNextTimeoutTimestamp();
    }
}
