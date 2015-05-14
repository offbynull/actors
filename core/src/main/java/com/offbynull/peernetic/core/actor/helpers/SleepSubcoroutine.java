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
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

/**
 * A subcoroutine that "sleeps" for a duration of time.
 * <p>
 * Works by requesting a timer to send a message after a certain duration of time. Use this for situations when you want to create an
 * artificial pause in your coroutine, such as when you want to avoid hammering out messages.
 * @author Kasra Faghihi
 */
public final class SleepSubcoroutine implements Subcoroutine<Void> {
    private final Address id;
    private final Address timerAddressPrefix;
    private final Duration timeoutDuration;

    private SleepSubcoroutine(Address id, Address timerAddressPrefix, Duration timeoutDuration) {
        Validate.notNull(id);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(timeoutDuration);
        Validate.isTrue(!timeoutDuration.isNegative());
        this.id = id;
        this.timerAddressPrefix = timerAddressPrefix;
        this.timeoutDuration = timeoutDuration;
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Object timeoutMarker = new Object();
        
        ctx.addOutgoingMessage(
                id,
                timerAddressPrefix.appendSuffix("" + timeoutDuration.toMillis()),
                timeoutMarker);
        
        Object incomingMessage;
        do {
            cnt.suspend();
            incomingMessage = ctx.getIncomingMessage();
        } while (incomingMessage != timeoutMarker);
        
        return null;
    }

    @Override
    public Address getId() {
        return id;
    }
    
    /**
     * {@link SleepSubcoroutine} builder. All validation is done in {@link #build() }.
     */
    public static final class Builder {
        private Address id;
        private Address timerAddressPrefix;
        private Duration duration;

        /**
         * Set the id. Defaults to {@code null}.
         * @param id id
         * @return this builder
         */
        public Builder id(Address id) {
            this.id = id;
            return this;
        }

        /**
         * Set the address to {@link TimerGateway}. Defaults to {@code null}.
         * @param timerAddressPrefix timer gateway address
         * @return this builder
         */
        public Builder timerAddressPrefix(Address timerAddressPrefix) {
            this.timerAddressPrefix = timerAddressPrefix;
            return this;
        }

        /**
         * Set the sleep duration. Defaults to {@code null}.
         * @param duration sleep duration
         * @return this builder
         */
        public Builder duration(Duration duration) {
            this.duration = duration;
            return this;
        }
        
        /**
         * Build a {@link SleepSubcoroutine} instance.
         * @return a new instance of {@link SleepSubcoroutine}
         * @throws NullPointerException if any parameters are {@code null}
         * @throws IllegalArgumentException if {@code duration} parameter was set to a negative duration, or if {@code id} was set to empty
         */
        public SleepSubcoroutine build() {
            return new SleepSubcoroutine(id, timerAddressPrefix, duration);
        }
        
    }
    
}
