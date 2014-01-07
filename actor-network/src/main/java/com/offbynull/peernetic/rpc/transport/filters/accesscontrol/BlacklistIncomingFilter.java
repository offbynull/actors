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
package com.offbynull.peernetic.rpc.transport.filters.accesscontrol;

import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;

/**
 * An {@link IncomingFilter} that blocks a collection of addresses.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class BlacklistIncomingFilter<A> implements IncomingFilter<A> {
    private Set<A> disallowedSet;

    /**
     * Constructs a {@link BlacklistIncomingFilter} object.
     */
    public BlacklistIncomingFilter() {
        this(Collections.<A>emptySet());
    }

    /**
     * Constructs a {@link BlacklistIncomingFilter} object with an initial set of addresses.
     * @param disallowedSet addresses to block
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public BlacklistIncomingFilter(Set<A> disallowedSet) {
        Validate.noNullElements(disallowedSet);
        
        this.disallowedSet = Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>());
        this.disallowedSet.addAll(disallowedSet);
    }

    /**
     * Add an address.
     * @param e address
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void addAddress(A e) {
        Validate.notNull(e);
        disallowedSet.add(e);
    }

    /**
     * Remove an address.
     * @param e address
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void removeAddress(A e) {
        Validate.notNull(e);
        disallowedSet.remove(e);
    }

    /**
     * Add a collection of addresses.
     * @param c addresses to add
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void addAddresses(Collection<? extends A> c) {
        Validate.noNullElements(c);
        disallowedSet.addAll(c);
    }

    /**
     * Remove a collection of addresses.
     * @param c addresses to remove
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void removeAddresses(Collection<? extends A> c) {
        Validate.noNullElements(c);
        disallowedSet.removeAll(c);
    }

    /**
     * Clears the blocked addresses.
     */
    public void clear() {
        disallowedSet.clear();
    }
    
    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        if (disallowedSet.contains(from)) {
            throw new AddressInBlacklistException();
        }
        
        return buffer;
    }
    
    /**
     * Exception thrown when {@link BlacklistIncomingFilter#filter(java.lang.Object, java.nio.ByteBuffer) } notices that the message comes
     * from a blacklisted address.
     */
    public static class AddressInBlacklistException extends RuntimeException {
    }
}
