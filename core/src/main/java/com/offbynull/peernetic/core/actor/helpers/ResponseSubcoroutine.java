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
import com.offbynull.peernetic.core.actor.BatchedOutgoingMessage;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableObject;

/**
 * A subcoroutine that acts as a filter for another subcoroutine, caching outgoing responses to requests and resending those responses if
 * the same request comes in again (within a certain time span).
 * <p>
 * When using this subcoroutine, keep in mind the following:
 * <ul>
 * <li>All incoming messages are cached.</li>
 * <li>The source address of an incoming message is used to test if a message has already been received.</li>
 * <li>Responses for a cached item are set only if those responses go out immediately. If you delay sending out a response, this
 * subcoroutine will think that you sent out no response.</li>
 * </ul>
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
        Map<Address, List<BatchedOutgoingMessage>> cache = new HashMap<>(); // sourceaddress -> responses
        while (true) {
            Object msg = ctx.getIncomingMessage();
            Address src = ctx.getSource();

            // If msg is to discard a message sitting in the cache, then discard and continue
            if (msg instanceof ResponseSubcoroutine.RemoveFromCache) {
                @SuppressWarnings("unchecked")
                RemoveFromCache removeMessage = (RemoveFromCache) msg;
                Validate.isTrue(removeMessage.getParent() == this); // sanity check

                Address sourceToRemoveFromCache = removeMessage.getSource();
                cache.remove(sourceToRemoveFromCache);

                cnt.suspend();
                continue;
            }

            // If already in cache, send cached responses... don't bother forwarding to backing subcoroutine
            if (cache.containsKey(src)) {
                List<BatchedOutgoingMessage> cachedResponses = cache.get(src);
                for (BatchedOutgoingMessage cachedResponse : cachedResponses) {
                    ctx.addOutgoingMessage(cachedResponse.getSourceId(), cachedResponse.getDestination(), cachedResponse.getMessage());
                }
                cnt.suspend();
                continue;
            }

            // Ask timer to send us a msg to remove this msg from the cache after retainDuration + add msg to cache
            ctx.addOutgoingMessage(timerAddressPrefix.appendSuffix("" + retainDuration.toMillis()), new RemoveFromCache(src));

            // Execute backing subcourtine, and determine if any new messages were added in. If there were new messages added in, pick out
            // the ones that are going back to the sender and cache them (those ones are the responses, assuming that the request came from
            // a unique address that will continue to use the same unique address for retries of the request)
            int msgSizeBeforeRun = ctx.viewOutgoingMessages().size();
            if (!runner.execute()) {
                return ret.getValue();
            }
            int msgSizeAfterRun = ctx.viewOutgoingMessages().size();

            List<BatchedOutgoingMessage> newMsgs = new ArrayList<>(ctx.viewOutgoingMessages().subList(msgSizeBeforeRun, msgSizeAfterRun));
            newMsgs = newMsgs.stream().filter(x -> x.getDestination().equals(src)).collect(Collectors.toList());
            cache.put(src, newMsgs); // This is the correct behaviour even if list is empty -- if empty, it means we had no responses for
                                     // the request, but we still ingested the message. As such, if the same msg comes in don't hit it again

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
