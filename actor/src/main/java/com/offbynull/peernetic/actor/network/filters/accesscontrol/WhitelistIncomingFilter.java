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
package com.offbynull.peernetic.actor.network.filters.accesscontrol;

import com.offbynull.peernetic.actor.network.IncomingFilter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.Validate;

/**
 * An {@link IncomingFilter} that only allows a collection of addresses.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class WhitelistIncomingFilter<A> implements IncomingFilter<A> {

    private Set<A> allowedSet;

    /**
     * Constructs a {@link WhitelistIncomingFilter} object.
     */
    public WhitelistIncomingFilter() {
        this(Collections.<A>emptySet());
    }

    /**
     * Constructs a {@link WhitelistIncomingFilter} object with an initial set of addresses.
     * @param allowedSet addresses to block
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public WhitelistIncomingFilter(Set<A> allowedSet) {
        Validate.noNullElements(allowedSet);

        this.allowedSet = Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>());
        this.allowedSet.addAll(allowedSet);
    }

    /**
     * Add an address.
     * @param e address
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void addAddress(A e) {
        Validate.notNull(e);
        allowedSet.add(e);
    }

    /**
     * Remove an address.
     * @param e address
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void removeAddress(A e) {
        Validate.notNull(e);
        allowedSet.remove(e);
    }

    /**
     * Add a collection of addresses.
     * @param c addresses to add
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void addAddresses(Collection<? extends A> c) {
        Validate.noNullElements(c);
        allowedSet.addAll(c);
    }

    /**
     * Remove a collection of addresses.
     * @param c addresses to remove
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void removeAddresses(Collection<? extends A> c) {
        Validate.noNullElements(c);
        allowedSet.removeAll(c);
    }

    /**
     * Clears the blocked addresses.
     */
    public void clear() {
        allowedSet.clear();
    }

    @Override
    public ByteBuffer filter(A from, ByteBuffer buffer) {
        if (!allowedSet.contains(from)) {
            throw new AddressNotInWhitelistException();
        }
        
        return buffer;
    }

    /**
     * Exception thrown when {@link WhitelistIncomingFilter#filter(java.lang.Object, java.nio.ByteBuffer) } notices that the message doesn't
     * come from a whitelisted address.
     */
    public static class AddressNotInWhitelistException extends RuntimeException {
    }
}
