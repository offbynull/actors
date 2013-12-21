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
package com.offbynull.rpc;

import com.offbynull.rpc.invoke.InvokeThreadInformation;
import com.offbynull.rpc.transport.IncomingFilter;
import com.offbynull.rpc.transport.OutgoingFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * RPC configuration.
 * @author Kasra F
 * @param <A> address type
 */
public final class RpcConfig<A> {
    
    private ExecutorService invokerExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    private Map<? extends Object, ? extends Object> extraInvokeInfo = Collections.emptyMap();
    private List<IncomingFilter<A>> incomingFilters = Collections.emptyList();
    private List<OutgoingFilter<A>> outgoingFilters = Collections.emptyList();

    /**
     * Get the {@link ExecutorService} to be used by the method invoker.
     * @return invoker {@link ExecutorService}
     */
    public ExecutorService getInvokerExecutorService() {
        return invokerExecutorService;
    }

    /**
     * Set the {@link ExecutorService} to be used by the method invoker. Shuts down the previously set {@link ExecutorService}.
     * @param invokerExecutorService invoker {@link ExecutorService}
     * @throws NullPointerException if any arguments are {@code null}
     */
    public void setInvokerExecutorService(ExecutorService invokerExecutorService) {
        Validate.notNull(invokerExecutorService);
        this.invokerExecutorService.shutdownNow();
        this.invokerExecutorService = invokerExecutorService;
    }

    /**
     * Get the extra key-value pairs to be passed to {@link InvokeThreadInformation} on each method invokation.
     * @return extra invokation data
     */
    public Map<? extends Object, ? extends Object> getExtraInvokeInfo() {
        return extraInvokeInfo;
    }

    /**
     * Set the extra key-value pairs to be passed to {@link InvokeThreadInformation} on each method invokation.
     * @param invokeDataMap extra invokation data
     * @throws NullPointerException if any arguments are null
     */
    public void setExtraInvokeInfo(Map<? extends Object, ? extends Object> invokeDataMap) {
        Validate.notNull(invokeDataMap);
        for (RpcInvokeKeys key : RpcInvokeKeys.values()) {
            Validate.isTrue(!invokeDataMap.keySet().contains(key));
        }
        
        this.extraInvokeInfo = Collections.unmodifiableMap(new HashMap<>(invokeDataMap));
    }

    /**
     * Get {@link IncomingFilter}s to be applied to incoming data.
     * @return outgoing filters
     */
    public List<IncomingFilter<A>> getIncomingFilters() {
        return incomingFilters;
    }

    /**
     * Get {@link IncomingFilter}s to be applied to incoming data.
     * @param incomingFilters incoming filters
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setIncomingFilters(List<IncomingFilter<A>> incomingFilters) {
        Validate.noNullElements(incomingFilters);
        this.incomingFilters = Collections.unmodifiableList(new ArrayList<>(incomingFilters));
    }

    /**
     * Get {@link OutgoingFilter}s to be applied to outgoing data.
     * @return outgoing filters
     */
    public List<OutgoingFilter<A>> getOutgoingFilters() {
        return outgoingFilters;
    }

    /**
     * Set {@link OutgoingFilter}s to be applied to outgoing data.
     * @param outgoingFilters outgoing filters
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setOutgoingFilters(List<OutgoingFilter<A>> outgoingFilters) {
        Validate.noNullElements(outgoingFilters);
        this.outgoingFilters = Collections.unmodifiableList(new ArrayList<>(outgoingFilters));
    }
    
}
