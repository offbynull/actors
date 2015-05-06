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
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * A subcoroutine that sends a request and waits for a response. The request may be attempted multiple times before giving up -- a timer
 * is used to sleep between resends.
 * <p>
 * Response is returned by the subcoroutine.
 * @author Kasra Faghihi
 * @param <T> response type
 */
public final class RequestSubcoroutine<T> implements Subcoroutine<T> {
    private final String id;
    private final String destinationAddress;
    private final Object request;
    private final String timerAddressPrefix;
    private final int maxAttempts;
    private final Duration attemptInterval;
    private final Set<Class<?>> expectedResponseTypes;
    private final boolean exceptionOnNoResponse;
    private Object response;
    
    private RequestSubcoroutine(String id,
            String destinationAddress,
            String timerAddressPrefix,
            Object request,
            int maxAttempts,
            Duration attemptInterval,
            Set<Class<?>> expectedResponseTypes,
            boolean exceptionOnNoResponse) {
        Validate.notNull(id);
        Validate.notNull(destinationAddress);
        Validate.notNull(request);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(attemptInterval);
        Validate.notNull(expectedResponseTypes);
        Validate.isTrue(maxAttempts > 0);
        Validate.isTrue(!attemptInterval.isNegative());
        this.id = id;
        this.destinationAddress = destinationAddress;
        this.request = request;
        this.timerAddressPrefix = timerAddressPrefix;
        this.maxAttempts = maxAttempts;
        this.attemptInterval = attemptInterval;
        this.expectedResponseTypes = new HashSet<>(expectedResponseTypes);
        this.exceptionOnNoResponse = exceptionOnNoResponse;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public T run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        reattempt:
        for (int i = 0; i < maxAttempts; i++) {
            Object timeoutMarker = new Object();

            ctx.addOutgoingMessage(
                    id,
                    destinationAddress,
                    request);
            ctx.addOutgoingMessage(
                    id,
                    AddressUtils.parentize(timerAddressPrefix, "" + attemptInterval.toMillis()),
                    timeoutMarker);
            
            while (true) {
                cnt.suspend();

                Object incomingMessage = ctx.getIncomingMessage();

                // If timedout, reattempt
                if (AddressUtils.isPrefix(timerAddressPrefix, ctx.getSource()) && incomingMessage == timeoutMarker) {
                    continue reattempt;
                }

                // If not a response from location we sent to
                if (ctx.getSource().equals(destinationAddress) && incomingMessage == timeoutMarker) {
                    continue reattempt;
                }
                
                // If not of expected response type
                boolean found = false;
                for (Class<?> type : expectedResponseTypes) {
                    if (ClassUtils.isAssignable(incomingMessage.getClass(), type)) {
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    continue;
                }
                
                // This is our response. Set it and leave.
                response = incomingMessage;
                break reattempt;
            }
        }
        
        if (exceptionOnNoResponse && response == null) {
            throw new IllegalStateException();
        }
        
        return (T) response;
    }

    @Override
    public String getId() {
        return id;
    }
    
    /**
     * {@link RequestSubcoroutine} builder. All validation is done in {@link #build() }.
     * @param <T> expected return type
     */
    public static final class Builder<T> {
        private String id;
        private String destinationAddress;
        private Object request;
        private String timerAddressPrefix;
        private int maxAttempts = 5;
        private Duration attemptInterval = Duration.ofSeconds(2L);
        private Set<Class<?>> expectedResponseTypes = new HashSet<>();
        private boolean throwExceptionIfNoResponse = true;

        /**
         * Set the id. Defaults to {@code null}.
         * @param id id
         * @return this builder
         */
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the destination address for the request. Defaults to {@code null}.
         * @param destinationAddress destination address
         * @return this builder
         */
        public Builder<T> destinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
            return this;
        }

        /**
         * Set the request. Defaults to {@code null}.
         * @param request request to send
         * @return this builder
         */
        public Builder<T> request(Object request) {
            this.request = request;
            return this;
        }

        /**
         * Set the address to {@link TimerGateway}. Defaults to {@code null}.
         * @param timerAddressPrefix timer gateway address
         * @return this builder
         */
        public Builder<T> timerAddressPrefix(String timerAddressPrefix) {
            this.timerAddressPrefix = timerAddressPrefix;
            return this;
        }

        /**
         * Set the maximum number of times to attempt a request. Defaults to {@code 5}.
         * @param maxAttempts maximum number of times to attempt a request
         * @return this builder
         */
        public Builder<T> maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Set the amount of time to wait inbetween attempts. Defaults to 2 seconds.
         * @param attemptInterval amount of time to wait inbetween attempts
         * @return this builder
         */
        public Builder<T> attemptInterval(Duration attemptInterval) {
            this.attemptInterval = attemptInterval;
            return this;
        }

        /**
         * Set the expected response types. Defaults to empty set.
         * @param expectedResponseTypes expected response types
         * @return this builder
         * @throws NullPointerException if any argument is {@code null}
         */
        public Builder<T> expectedResponseTypes(Set<Class<?>> expectedResponseTypes) {
            this.expectedResponseTypes = new HashSet<>(expectedResponseTypes);
            return this;
        }

        /**
         * Add one or more expected response types.
         * @param expectedResponseType new expected response types to add
         * @return this builder
         * @throws NullPointerException if any argument is {@code null}
         */
        public Builder<T> addExpectedResponseType(Class<?> ... expectedResponseType) {
            expectedResponseTypes.addAll(Arrays.asList(expectedResponseType));
            return this;
        }

        /**
         * Set "throw exception if no response" flag. Throws an exception if we didn't end up getting a response of type we expect.
         * Exception is a {@link IllegalStateException}. Defaults to {@code true}.
         * @param throwExceptionIfNoResponse "throw exception if no response" flag
         * @return this builder
         */
        public Builder<T> throwExceptionIfNoResponse(boolean throwExceptionIfNoResponse) {
            this.throwExceptionIfNoResponse = throwExceptionIfNoResponse;
            return this;
        }
        
        /**
         * Build a {@link RequestSubcoroutine} instance.
         * @return a new instance of {@link RequestSubcoroutine}
         * @throws NullPointerException if any parameters are {@code null}
         * @throws IllegalArgumentException if {@code attemptInterval} parameter was set to a negative duration, or if {@code maxAttempts}
         * was set to 0
         */
        public RequestSubcoroutine<T> build() {
            return new RequestSubcoroutine<>(id, destinationAddress, timerAddressPrefix, request, maxAttempts, attemptInterval,
                    expectedResponseTypes, throwExceptionIfNoResponse);
        }
    }
}
