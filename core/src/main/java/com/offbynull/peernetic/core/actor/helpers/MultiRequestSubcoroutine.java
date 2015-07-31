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
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * A subcoroutine that sends a request to multiple recipients and waits for each recipient to respond. The request may be attempted multiple
 * times before giving up -- a timer is used to sleep between resends.
 * <p>
 * Response is returned by the subcoroutine.
 * @author Kasra Faghihi
 * @param <T> response type
 */
public final class MultiRequestSubcoroutine<T> implements Subcoroutine<List<Response<T>>> {
    private final Address id;
    private final Address timerAddressPrefix;
    private final Object request;
    private final int maxAttempts;
    private final Duration attemptInterval;
    private final Set<Class<?>> expectedResponseTypes;
    private final IndividualResponseListener<T> individualResponseListener;
    
    private final Map<String, Address> destinations; // key = unique source address suffix, value = destination address
    
    private MultiRequestSubcoroutine(
            Address id,
            Map<String, Address> destinations,
            Address timerAddressPrefix,
            Object request,
            int maxAttempts,
            Duration attemptInterval,
            Set<Class<?>> expectedResponseTypes,
            IndividualResponseListener<T> individualResponseListener) {
        Validate.notNull(id);
        Validate.notNull(destinations);
        Validate.notNull(request);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(attemptInterval);
        Validate.notNull(expectedResponseTypes);
        Validate.notNull(individualResponseListener);
        Validate.isTrue(!id.isEmpty());
        destinations.entrySet().forEach(x -> {
            Validate.notNull(x.getKey());
            Validate.notNull(x.getValue());
        });
        Validate.isTrue(!timerAddressPrefix.isEmpty());
        Validate.isTrue(maxAttempts > 0);
        Validate.isTrue(!attemptInterval.isNegative());
        this.id = id;
        this.destinations = new HashMap<>(destinations);
        this.request = request;
        this.timerAddressPrefix = timerAddressPrefix;
        this.maxAttempts = maxAttempts;
        this.attemptInterval = attemptInterval;
        this.expectedResponseTypes = new HashSet<>(expectedResponseTypes);
        this.individualResponseListener = individualResponseListener;
    }

    @Override
    public List<Response<T>> run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        if (destinations.isEmpty()) { // if no recipients, return immediately
            return new ArrayList<>();
        }
        
        Address routerAddress = id.appendSuffix("mrsr"); // mrsr = multirequestsubcoroutinerouter
        SubcoroutineRouter router = new SubcoroutineRouter(routerAddress, ctx);
        
        RequestSubcoroutine.Builder<T> baseRequestBuilder = new RequestSubcoroutine.Builder<T>()
                .request(request)
                .maxAttempts(maxAttempts)
                .attemptInterval(attemptInterval)
                .timerAddress(timerAddressPrefix)
                .expectedResponseTypes(expectedResponseTypes)
                .throwExceptionIfNoResponse(false);
        
        Map<Subcoroutine<?>, String> suffixMap = new HashMap<>(); // key = source address suffix, value = request subcoroutine
        for (Entry<String, Address> destinationEntry : destinations.entrySet()) {
            String uniqueSourceAddressSuffix = destinationEntry.getKey();
            RequestSubcoroutine<T> requestSubcoroutine = baseRequestBuilder
                    .address(routerAddress.appendSuffix(uniqueSourceAddressSuffix))
                    .destinationAddress(destinationEntry.getValue())
                    .build();
            router.getController().add(requestSubcoroutine, ADD_PRIME_NO_FINISH);
            suffixMap.put(requestSubcoroutine, uniqueSourceAddressSuffix);
        }
        
        List<Response<T>> ret = new ArrayList<>(destinations.size());
        while (true) {
            cnt.suspend();
            
            ForwardResult fr = router.forward();
            
            if (fr.isForwarded() && fr.isCompleted()) { // calling isCompleted by itself may throw an exception, check isForwarded first
                Object result = fr.getResult();
                
                if (result != null) { // null means no response, you can't pass null back as response, the system doesn't/shouldn't allow it
                    Subcoroutine<?> subcoroutine = fr.getSubcoroutine();

                    String uniqueSourceAddressSuffix = suffixMap.get(subcoroutine);
                    Address dstAddress = subcoroutine.getAddress();

                    @SuppressWarnings("unchecked")
                    Response<T> response = new Response<>(uniqueSourceAddressSuffix, dstAddress, (T) result);
                    
                    IndividualResponseAction action = individualResponseListener.responseArrived(response);
                    
                    if (action.isAdd()) {
                        ret.add(response);
                    }
                    
                    if (action.isStop()) {
                        break;
                    }
                }
            }
            
            if (router.getController().size() == 0) {
                break;
            }
        }
        
        return ret;
    }
    
    @Override
    public Address getAddress() {
        return id;
    }

    /**
     * Container for response.
     * @param <T> response type
     */
    public static final class Response<T> {
        private final String uniqueSourceAddressSuffix;
        private final Address destinationAddress;
        private final T response;

        private Response(String uniqueSourceAddressSuffix, Address destinationAddress, T response) {
            Validate.notNull(uniqueSourceAddressSuffix);
            Validate.notNull(destinationAddress);
            Validate.notNull(response);
            
            this.uniqueSourceAddressSuffix = uniqueSourceAddressSuffix;
            this.destinationAddress = destinationAddress;
            this.response = response;
        }

        /**
         * The unique source address suffix used for the request that got back this response.
         * @return unique source address suffix
         */
        public String getUniqueSourceAddressSuffix() {
            return uniqueSourceAddressSuffix;
        }

        /**
         * The destination address of the request that got back this response.
         * @return destination address of request
         */
        public Address getDestinationAddress() {
            return destinationAddress;
        }

        /**
         * The response.
         * @return response
         */
        public T getResponse() {
            return response;
        }
    }
    
    
    /**
     * {@link MultiRequestSubcoroutine} builder. All validation is done in {@link #build() }.
     * @param <T> expected return type
     */
    public static final class Builder<T> {
        private Address id;
        private List<ImmutablePair<String, Address>> destinations = new ArrayList<>();
        private Address timerAddressPrefix;
        private Object request;
        private int maxAttempts = 5;
        private Duration attemptInterval = Duration.ofSeconds(2L);
        private Set<Class<?>> expectedResponseTypes = new HashSet<>();
        private IndividualResponseListener<T> individualResponseListener = new DefaultIndividualResponseListener<>();

        /**
         * Set the address. The address set by this method must be relative to the calling actor's self address (relative to
         * {@link Context#getSelf()}). Defaults to {@code null}.
         * @param address relative address
         * @return this builder
         */
        public Builder<T> address(Address address) {
            this.id = address;
            return this;
        }

        /**
         * Add a destination for the request.
         * @param uniqueSourceAddressSuffix unique source address
         * @param destinationAddress destination address
         * @return this builder
         */
        public Builder<T> addDestination(String uniqueSourceAddressSuffix, Address destinationAddress) {
            destinations.add(new ImmutablePair<>(uniqueSourceAddressSuffix, destinationAddress));
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
        public Builder<T> timerAddress(Address timerAddressPrefix) {
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
         * Set the individual response listener. Defaults to {@link DefaultIndividualResponseListener}.
         * @param individualResponseListener individual response listener
         * @return this builder
         */
        public Builder<T> individualResponseListener(IndividualResponseListener<T> individualResponseListener) {
            this.individualResponseListener = individualResponseListener;
            return this;
        }

        /**
         * Build a {@link MultiRequestSubcoroutine} instance.
         * @return a new instance of {@link MultiRequestSubcoroutine}
         * @throws NullPointerException if any parameters are {@code null}, or contain {@code null}
         * @throws IllegalArgumentException if a duplicate source address suffix was used for a {@code destination}, or if
         * {@code attemptInterval} parameter was set to a negative duration, or if {@code maxAttempts} was set to {@code 0}, or if either
         * {@code address} or {@code destinationAddress} or {@code timeAddressPrefix} is set to empty
         */
        public MultiRequestSubcoroutine<T> build() {
            Map<String, Address> destinationsMap = destinations.stream().collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
            Validate.isTrue(destinationsMap.size() == destinations.size()); // check for no dupes
            return new MultiRequestSubcoroutine<>(id, destinationsMap, timerAddressPrefix, request, maxAttempts, attemptInterval,
                    expectedResponseTypes, individualResponseListener);
        }
    }
    
    /**
     * Used to filter/process responses individually as they come in to a {@link MultiRequestSubcoroutine}.
     * @param <T> response type
     */
    public interface IndividualResponseListener<T> {
        /**
         * Called by {@link MultiRequestSubcoroutine } when a response arrives.
         * @param response incoming response
         * @return action to have the calling {@link MultiRequestSubcoroutine } perform with this response
         * @throws NullPointerException if any argument is {@code null}
         */
        IndividualResponseAction responseArrived(Response<T> response);
    }
    
    /**
     * Returned by
     * {@link IndividualResponseListener#responseArrived(com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response) } to
     * instruct the calling {@link MultiRequestSubcoroutine} on what to do.
     */
    public static final class IndividualResponseAction {
        private final boolean add;
        private final boolean stop;

        /**
         * Constructs a {@link IndividualResponseAction} instance.
         * @param add if {@code true} adds the response to the list of responses that get returned by the calling
         * {@link MultiRequestSubcoroutine#run(com.offbynull.coroutines.user.Continuation) }
         * @param stop if {@code true} immediately stops the calling
         * {@link MultiRequestSubcoroutine#run(com.offbynull.coroutines.user.Continuation) } and has it return the list of responses its
         * accumulated thus far
         */
        public IndividualResponseAction(boolean add, boolean stop) {
            this.add = add;
            this.stop = stop;
        }

        private boolean isAdd() {
            return add;
        }

        private boolean isStop() {
            return stop;
        }
    }
    
    /**
     * An implementation of {@link IndividualResponseListener} that always adds a response to the list of incoming responses.
     * @param <T> response type
     */
    public static final class DefaultIndividualResponseListener<T> implements IndividualResponseListener<T> {

        @Override
        public IndividualResponseAction responseArrived(Response<T> response) {
            Validate.notNull(response);
            return new IndividualResponseAction(true, false);
        }
        
    }
}
