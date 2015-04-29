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
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public final class RequestSubcoroutine<T> implements Subcoroutine<T> {
    private final String sourceId;
    private final String destinationAddress;
    private final Object request;
    private final String timerAddressPrefix;
    private final int maxAttempts;
    private final Duration attemptInterval;
    private final Set<Class<?>> expectedResponseTypes;
    private final boolean exceptionOnNoResponse;
    private Object response;
    
    private RequestSubcoroutine(String sourceId,
            String destinationAddress,
            String timerAddressPrefix,
            Object request,
            int maxAttempts,
            Duration attemptInterval,
            Set<Class<?>> expectedResponseTypes,
            boolean exceptionOnNoResponse) {
        Validate.notNull(sourceId);
        Validate.notNull(destinationAddress);
        Validate.notNull(request);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(attemptInterval);
        Validate.notNull(expectedResponseTypes);
        Validate.isTrue(maxAttempts > 0);
        Validate.isTrue(!attemptInterval.isNegative());
        this.sourceId = sourceId;
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
                    sourceId,
                    destinationAddress,
                    request);
            ctx.addOutgoingMessage(
                    sourceId,
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
    public String getSourceId() {
        return sourceId;
    }
    
    public static final class Builder<T> {
        private String sourceId;
        private String destinationAddress;
        private Object request;
        private String timerAddressPrefix;
        private int maxAttempts = 5;
        private Duration attemptInterval = Duration.ofSeconds(2L);
        private Set<Class<?>> expectedResponseTypes = new HashSet<>();
        private boolean throwExceptionIfNoResponse = true;

        public Builder<T> sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder<T> destinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
            return this;
        }

        public Builder<T> request(Object request) {
            this.request = request;
            return this;
        }

        public Builder<T> timerAddressPrefix(String timerAddressPrefix) {
            this.timerAddressPrefix = timerAddressPrefix;
            return this;
        }

        public Builder<T> maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder<T> attemptInterval(Duration attemptInterval) {
            this.attemptInterval = attemptInterval;
            return this;
        }

        public Builder<T> expectedResponseTypes(Set<Class<?>> expectedResponseTypes) {
            this.expectedResponseTypes = new HashSet<>(expectedResponseTypes);
            return this;
        }

        public Builder<T> addExpectedResponseType(Class<?> ... expectedResponseType) {
            expectedResponseTypes.addAll(Arrays.asList(expectedResponseType));
            return this;
        }
        
        public Builder<T> throwExceptionIfNoResponse(boolean throwExceptionIfNoResponse) {
            this.throwExceptionIfNoResponse = throwExceptionIfNoResponse;
            return this;
        }
        
        public RequestSubcoroutine<T> build() {
            try {
                return new RequestSubcoroutine<T>(sourceId, destinationAddress, timerAddressPrefix, request, maxAttempts, attemptInterval,
                        expectedResponseTypes, throwExceptionIfNoResponse);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
