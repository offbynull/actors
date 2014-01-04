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
package com.offbynull.peernetic.rpc;

import com.offbynull.peernetic.rpc.invoke.AsyncCapturer;
import com.offbynull.peernetic.rpc.invoke.AsyncCapturerFactory;
import com.offbynull.peernetic.rpc.invoke.Capturer;
import com.offbynull.peernetic.rpc.invoke.CapturerFactory;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import com.offbynull.peernetic.rpc.invoke.Invoker;
import com.offbynull.peernetic.rpc.invoke.InvokerFactory;
import com.offbynull.peernetic.rpc.invoke.capturers.cglib.CgLibAsyncCapturerFactory;
import com.offbynull.peernetic.rpc.invoke.capturers.cglib.CgLibCapturerFactory;
import com.offbynull.peernetic.rpc.invoke.invokers.reflection.ReflectionInvokerFactory;
import com.offbynull.peernetic.rpc.transport.IncomingFilter;
import com.offbynull.peernetic.rpc.transport.OutgoingFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * RPC configuration.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class RpcConfig<A> {

    private InvokerFactory invokerFactory = new ReflectionInvokerFactory();
    private CapturerFactory capturerFactory = new CgLibCapturerFactory();
    private AsyncCapturerFactory asyncCapturerFactory = new CgLibAsyncCapturerFactory();
    private Map<? extends Object, ? extends Object> extraInvokeInfo = Collections.emptyMap();
    private List<IncomingFilter<A>> incomingFilters = Collections.emptyList();
    private List<OutgoingFilter<A>> outgoingFilters = Collections.emptyList();

    InvokerFactory getInvokerFactory() {
        return invokerFactory;
    }

    /**
     * Set the invoker factory for creating {@link Invoker}s that performs RPC method invocations.
     * @param invokerFactory invoker factory
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setInvokerFactory(InvokerFactory invokerFactory) {
        Validate.notNull(invokerFactory);
        this.invokerFactory = invokerFactory;
    }

    CapturerFactory getCapturerFactory() {
        return capturerFactory;
    }

    /**
     * Set the capturer factory for creating {@link Capturer}s that proxy RPC service interfaces.
     * @param capturerFactory capturer factory
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setCapturerFactory(CapturerFactory capturerFactory) {
        Validate.notNull(capturerFactory);
        this.capturerFactory = capturerFactory;
    }

    AsyncCapturerFactory getAsyncCapturerFactory() {
        return asyncCapturerFactory;
    }

    /**
     * Set the async capturer factory for creating {@link AsyncCapturer}s that proxy RPC service async interfaces.
     * @param asyncCapturerFactory async capturer factory
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setAsyncCapturerFactory(AsyncCapturerFactory asyncCapturerFactory) {
        Validate.notNull(asyncCapturerFactory);
        this.asyncCapturerFactory = asyncCapturerFactory;
    }

    Map<? extends Object, ? extends Object> getExtraInvokeInfo() {
        return extraInvokeInfo;
    }

    /**
     * Set the extra key-value pairs to be passed to {@link InvokeThreadInformation} on each method invocation.
     * @param invokeDataMap extra invocation data
     * @throws NullPointerException if any arguments are null
     */
    public void setExtraInvokeInfo(Map<? extends Object, ? extends Object> invokeDataMap) {
        Validate.notNull(invokeDataMap);
        for (RpcInvokeKeys key : RpcInvokeKeys.values()) {
            Validate.isTrue(!invokeDataMap.keySet().contains(key));
        }
        
        this.extraInvokeInfo = Collections.unmodifiableMap(new HashMap<>(invokeDataMap));
    }

    List<IncomingFilter<A>> getIncomingFilters() {
        return incomingFilters;
    }

    /**
     * Get {@link IncomingFilter}s to be applied to incoming data.
     * @param incomingFilters incoming filters
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setIncomingFilters(List<? extends IncomingFilter<A>> incomingFilters) {
        Validate.noNullElements(incomingFilters);
        this.incomingFilters = Collections.unmodifiableList(new ArrayList<>(incomingFilters));
    }

    List<OutgoingFilter<A>> getOutgoingFilters() {
        return outgoingFilters;
    }

    /**
     * Set {@link OutgoingFilter}s to be applied to outgoing data.
     * @param outgoingFilters outgoing filters
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setOutgoingFilters(List<? extends OutgoingFilter<A>> outgoingFilters) {
        Validate.noNullElements(outgoingFilters);
        this.outgoingFilters = Collections.unmodifiableList(new ArrayList<>(outgoingFilters));
    }
    
}
