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
import com.offbynull.peernetic.actor.EndpointFinder;
import com.offbynull.peernetic.actor.EndpointKeyExtractor;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class LinkRepository<A> {

    private EndpointFinder<A> finder;
    private EndpointKeyExtractor<A> keyExtractor;
    
    private Set<A> incomingLinks = new HashSet<>();
    private Set<A> outgoingLinks = new HashSet<>();
    private Set<A> cacheLinks = new HashSet<>();
    private UnstructuredOverlayListener<A> listener;
    private UnstructuredOverlay<A> parent;

    public LinkRepository(UnstructuredOverlay<A> parent, UnstructuredOverlayListener<A> listener, EndpointFinder<A> finder,
            EndpointKeyExtractor<A> keyExtractor, Set<A> cacheLinks) {
        Validate.notNull(parent);
        Validate.notNull(listener);
        Validate.notNull(finder);
        Validate.notNull(keyExtractor);
        this.parent = parent;
        this.listener = listener;
        this.finder = finder;
        this.keyExtractor = keyExtractor;
        this.cacheLinks = new LinkedHashSet<>(cacheLinks);
    }

    public void addLink(LinkType type, Endpoint link) {
        Validate.notNull(type);
        Validate.notNull(link);
        
        A address = keyExtractor.findKey(link);
        Validate.notNull(address);

        switch (type) {
            case INCOMING:
                incomingLinks.add(address);
                cacheLinks.add(address);
                break;
            case OUTGOING:
                outgoingLinks.add(address);
                cacheLinks.add(address);
                break;
            default:
                throw new IllegalStateException();
        }
        
        try {
            listener.linkCreated(parent, type, address);
        } catch (RuntimeException re) { // NOPMD
        }
    }

    public void removeLink(LinkType type, Endpoint link) {
        Validate.notNull(type);
        Validate.notNull(link);
        
        A address = keyExtractor.findKey(link);
        Validate.notNull(address);

        switch (type) {
            case INCOMING:
                incomingLinks.remove(address);
                break;
            case OUTGOING:
                outgoingLinks.remove(address);
                break;
            default:
                throw new IllegalStateException();
        }
        
        try {
            listener.linkDestroyed(parent, type, address);
        } catch (RuntimeException re) { // NOPMD
        }
    }
    
    public boolean containsLink(LinkType type, Endpoint link) {
        Validate.notNull(type);
        Validate.notNull(link);
        
        A address = keyExtractor.findKey(link);
        Validate.notNull(address);

        switch (type) {
            case INCOMING:
                return incomingLinks.contains(address);
            case OUTGOING:
                return outgoingLinks.contains(address);
            default:
                throw new IllegalStateException();
        }
    }
    
    public State<A> toState() {
        return new State<>(incomingLinks, outgoingLinks);
    }
    
    
    public Endpoint peekNextCache() {
        Iterator<A> it = cacheLinks.iterator();
        if (it.hasNext()) {
            Endpoint endpoint = finder.findEndpoint(it.next());
            
            Validate.notNull(endpoint);
            return endpoint;
        }
        
        return null;
    }
    
    public Endpoint pollNextCache() {
        Iterator<A> it = cacheLinks.iterator();
        if (it.hasNext()) {
            Endpoint endpoint = finder.findEndpoint(it.next());
            
            it.remove();
            
            Validate.notNull(endpoint);
            return endpoint;
        }
        
        return null;
    }
}
