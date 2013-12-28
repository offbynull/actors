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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * The state of a node. Contains the node's links and a flag that indicates if the node has room for more incoming connections.
 * @author Kasra Faghihi
 * @param <A> address type
 */
final class State<A> {
    private Set<A> incomingLinks;
    private Set<A> outgoingLinks;
    private boolean incomingLinksFull;

    /**
     * Construct a {@link State} object.
     * @param incomingLinks incoming links
     * @param outgoingLinks outgoing links
     * @param incomingLinksFull flag indicating if the node can accept more incoming links
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public State(Set<A> incomingLinks, Set<A> outgoingLinks, boolean incomingLinksFull) {
        Validate.noNullElements(incomingLinks);
        Validate.noNullElements(outgoingLinks);

        this.incomingLinks = Collections.unmodifiableSet(new HashSet<>(incomingLinks));
        this.outgoingLinks = Collections.unmodifiableSet(new HashSet<>(outgoingLinks));
        this.incomingLinksFull = incomingLinksFull;
    }

    /**
     * Get incoming links.
     * @return unmodifiable incoming links
     */
    public Set<A> getIncomingLinks() {
        return incomingLinks;
    }

    /**
     * Get outgoing links.
     * @return unmodifiable outgoing links
     */
    public Set<A> getOutgoingLinks() {
        return outgoingLinks;
    }

    /**
     * Get if there's no more room available for incoming links.
     * @return {@code true} if there's no more room available for incoming links, {@code false} otherwise
     */
    public boolean isIncomingLinksFull() {
        return incomingLinksFull;
    }
    
}
