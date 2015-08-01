/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.Validate;

/**
 * A subcoroutine that wraps a normal {@link Coroutine}.
 * @author Kasra Faghihi
 */
public final class CoroutineSubcoroutine implements Subcoroutine<Void> {

    private final Address address;
    private final Coroutine coroutine;

    /**
     * Constructs a {@link CoroutineSubcoroutine} instance.
     * @param address address of this subcoroutine (relative to the calling actor's self address)
     * @param coroutine coroutine to wrap
     * @throws NullPointerException if any argument is {@code null}
     */
    public CoroutineSubcoroutine(Address address, Coroutine coroutine) {
        Validate.notNull(address);
        Validate.notNull(coroutine);
//        Validate.isTrue(!address.isEmpty()); // can be empty because it's relative?
        this.address = address;
        this.coroutine = coroutine;
    }
    
    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        // Dead store findbugs warnings are false positives caused by coroutine instrumentation
        coroutine.run(cnt);
        return null;
    }
}
