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

import com.offbynull.peernetic.actor.Actor;
import com.offbynull.peernetic.actor.ActorStartSettings;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.EndpointKeyExtractor;
import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PullQueue;
import com.offbynull.peernetic.actor.PushQueue;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * An unstructured overlay service.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class UnstructuredOverlay<A> extends Actor {
    private LinkRepository<A> linkRepository;
    private IncomingLinkManager<A> incomingLinkManager;
    private OutgoingLinkManager<A> outgoingLinkManager;
    
    /**
     * Constructs a {@link UnstructuredOverlay} object.
     * @param listener listener
     * @param finder find endpoints by key
     * @param extractor extract keys from endpoints
     * @param maxLinks maximum number of incoming/outgoing links allowed
     * @param expireDuration maximum amount of time before an incoming link expires
     * @param cache initial cache of addresses to connect to
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if any numeric argument is negative
     */
    public UnstructuredOverlay(UnstructuredOverlayListener listener, EndpointFinder<A> finder, EndpointKeyExtractor<A> extractor,
            int maxLinks, long expireDuration, Set<A> cache) {
        Validate.notNull(listener);
        Validate.notNull(finder);
        Validate.notNull(extractor);
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, expireDuration);
        
        long pendingDuration = expireDuration;
        long staleDuration = expireDuration / 2L;
        
        this.linkRepository = new LinkRepository<>(this, listener, finder, extractor, cache);
        this.incomingLinkManager = new IncomingLinkManager<>(maxLinks, expireDuration, linkRepository);
        this.outgoingLinkManager = new OutgoingLinkManager<>(linkRepository, maxLinks, staleDuration, expireDuration, pendingDuration);
    }

    
    @Override
    protected ActorStartSettings onStart(long timestamp, PushQueue pushQueue, Map<Object, Object> initVars) throws Exception {
        outgoingLinkManager.init(timestamp);
        return new ActorStartSettings(timestamp); // invoke onStep immediately
    }
    
    @Override
    protected long onStep(long timestamp, PullQueue pullQueue, PushQueue pushQueue, Endpoint selfEndpoint) throws Exception {
        Incoming incoming;
        while ((incoming = pullQueue.pull()) != null) {
            Endpoint from = incoming.getSource();
            Object content = incoming.getContent();
            
            incomingLinkManager.processCommand(timestamp, from, content, pushQueue);
            outgoingLinkManager.processCommand(timestamp, from, content, pushQueue);
        }
        

        long ilmNextTimestamp = incomingLinkManager.process(timestamp, pushQueue);
        long olmNextTimestamp = outgoingLinkManager.process(timestamp, pushQueue);
        
        long nextTimestamp = Long.MAX_VALUE;
        nextTimestamp = Math.min(nextTimestamp, ilmNextTimestamp);
        nextTimestamp = Math.min(nextTimestamp, olmNextTimestamp);
        
        return nextTimestamp;
    }
    
    @Override
    protected void onStop(long timestamp, PushQueue pushQueue) throws Exception {
    }
}
