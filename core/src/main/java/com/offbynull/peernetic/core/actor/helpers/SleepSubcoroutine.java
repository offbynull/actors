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
 * <p>
 * This class is not thread-safe / immutable.
 * @author Kasra Faghihi
 */
public final class SleepSubcoroutine implements Subcoroutine<Void> {
    private final Address sourceAddress;
    private final Address timerAddress;
    private final Duration timeoutDuration;

    private SleepSubcoroutine(Address sourceAddress, Address timerAddress, Duration timeoutDuration) {
        Validate.notNull(sourceAddress);
        Validate.notNull(timerAddress);
        Validate.notNull(timeoutDuration);
//        Validate.isTrue(!address.isEmpty()); // can be empty because it's relative?
        Validate.isTrue(!timerAddress.isEmpty());
        Validate.isTrue(!timeoutDuration.isNegative());
        this.sourceAddress = sourceAddress;
        this.timerAddress = timerAddress;
        this.timeoutDuration = timeoutDuration;
    }
    
    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Object timeoutMarker = new Object();
        
        ctx.addOutgoingMessage(sourceAddress,
                timerAddress.appendSuffix("" + timeoutDuration.toMillis()),
                timeoutMarker);
        
        Object incomingMessage;
        do {
            cnt.suspend();
            incomingMessage = ctx.getIncomingMessage();
        } while (incomingMessage != timeoutMarker);
        
        return null;
    }

    @Override
    public Address getAddress() {
        return sourceAddress;
    }
    
    /**
     * {@link SleepSubcoroutine} builder. All validation is done in {@link #build() }.
     */
    public static final class Builder {
        private Address sourceAddress;
        private Address timerAddress;
        private Duration duration;

        /**
         * Set the source address. The address set by this method must be relative to the calling actor's self address (relative to
         * {@link Context#getSelf()}). Defaults to {@code null}.
         * @param sourceAddress relative source address
         * @return this builder
         */
        public Builder sourceAddress(Address sourceAddress) {
            this.sourceAddress = sourceAddress;
            return this;
        }

        /**
         * Set the address to {@link TimerGateway}. Defaults to {@code null}.
         * @param timerAddressPrefix timer gateway address
         * @return this builder
         */
        public Builder timerAddress(Address timerAddressPrefix) {
            this.timerAddress = timerAddressPrefix;
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
         * @throws IllegalArgumentException if {@code duration} parameter was set to a negative duration, or if {@code timeAddress} was set
         * to empty
         */
        public SleepSubcoroutine build() {
            return new SleepSubcoroutine(sourceAddress, timerAddress, duration);
        }
        
    }
    
}
