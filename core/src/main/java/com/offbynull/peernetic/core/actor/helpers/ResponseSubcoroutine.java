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
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * A subcoroutine that acts as a filter for another subcoroutine, ignoring requests that have already been received before (within a certain
 * time span). The source address of an incoming message is used to test if a message has already been received.
 *
 * @author Kasra Faghihi
 * @param <T> result type of backing subcoroutine
 */
public final class ResponseSubcoroutine<T> implements Subcoroutine<T> {

    private final Address timerAddressPrefix;
    private final Duration retainDuration;
    private final Subcoroutine<T> backingSubcoroutine;

    /**
     * Constructs a {@link ResponseSubcoroutine} instance.
     *
     * @param timerAddressPrefix address of timer gateway
     * @param retainDuration duration of time to wait before clearing cache
     * @param backingSubcoroutine subcoroutine to forward messages to
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code retainDuration} is negative
     */
    public ResponseSubcoroutine(Address timerAddressPrefix, Duration retainDuration, Subcoroutine<T> backingSubcoroutine) {
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(retainDuration);
        Validate.notNull(backingSubcoroutine);
        Validate.isTrue(!retainDuration.isNegative());
        this.timerAddressPrefix = timerAddressPrefix;
        this.retainDuration = retainDuration;
        this.backingSubcoroutine = backingSubcoroutine;
    }

    @Override
    public Address getId() {
        return backingSubcoroutine.getId();
    }

    @Override
    public T run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        // Set up backing subcoroutine in an independent coroutine runner
        MutableObject<T> ret = new MutableObject<>();
        CoroutineRunner runner = new CoroutineRunner(x -> {
            T backingRet = backingSubcoroutine.run(x);
            ret.setValue(backingRet);
        });
        runner.setContext(ctx);

        // Check incoming message isn't a duplicate and forward to backing subcoroutine if it isn't
        Set<Address> sourceCache = new HashSet<>();
        while (true) {
            Object msg = ctx.getIncomingMessage();
            Address src = ctx.getSource();

            // If msg is to discard a message sitting in the cache, then discard and continue
            if (msg instanceof ResponseSubcoroutine.RemoveFromCache) {
                @SuppressWarnings("unchecked")
                RemoveFromCache removeMessage = (RemoveFromCache) msg;
                Validate.isTrue(removeMessage.getParent() == this); // sanity check

                Address sourceToRemoveFromCache = removeMessage.getSource();
                sourceCache.remove(sourceToRemoveFromCache);

                cnt.suspend();
                continue;
            }

            // Do not forward if message is already in cache
            if (sourceCache.contains(src)) {
                cnt.suspend();
                continue;
            }

            // Ask timer to send us a msg to remove this msg from the cache after retainDuration + add msg to cache
            ctx.addOutgoingMessage(timerAddressPrefix.appendSuffix("" + retainDuration.toMillis()), new RemoveFromCache(src));
            sourceCache.add(src);

            if (!runner.execute()) {
                return ret.getValue();
            }
            cnt.suspend();
        }
    }

    private final class RemoveFromCache {

        private Address source;

        public RemoveFromCache(Address source) {
            Validate.notNull(source);
            this.source = source;
        }

        public Address getSource() {
            return source;
        }

        public ResponseSubcoroutine<?> getParent() {
            return ResponseSubcoroutine.this;
        }

    }

}
