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
package com.offbynull.peernetic.rpc.invoke.invokers.reflection;

import com.offbynull.peernetic.rpc.invoke.Deserializer;
import com.offbynull.peernetic.rpc.invoke.Invoker;
import com.offbynull.peernetic.rpc.invoke.InvokerFactory;
import com.offbynull.peernetic.rpc.invoke.PostInvokeFilter;
import com.offbynull.peernetic.rpc.invoke.PreInvokeFilter;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import com.offbynull.peernetic.rpc.invoke.filters.sanity.AvoidObjectMethodsPreInvokeFilter;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamDeserializer;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamSerializer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

/**
 * A factory for {@link ReflectionInvoker} objects.
 * @author Kasra Faghihi
 */
public final class ReflectionInvokerFactory implements InvokerFactory {
    private ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private Serializer serializer = new XStreamSerializer();
    private Deserializer deserializer = new XStreamDeserializer();
    private List<? extends PreInvokeFilter> preInvokeFilters =
            Collections.unmodifiableList(Arrays.asList(new AvoidObjectMethodsPreInvokeFilter()));
    private List<? extends PostInvokeFilter> postInvokeFilters = Collections.emptyList();

    /**
     * Set the executor to created invokers should use.
     * @param executor executor
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code executor} is shutdown or terminated
     */
    public void setExecutor(ExecutorService executor) {
        Validate.notNull(executor);
        Validate.isTrue(!executor.isShutdown());
        Validate.isTrue(!executor.isTerminated());
        this.executor = executor;
    }

    /**
     * Set the serializer to created invokers should use.
     * @param serializer serializer
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setSerializer(Serializer serializer) {
        Validate.notNull(serializer);
        this.serializer = serializer;
    }

    /**
     * Set the deserializer to created invokers should use.
     * @param deserializer deserializer
     * @throws NullPointerException if any argument is {@code null}
     */
    public void setDeserializer(Deserializer deserializer) {
        Validate.notNull(deserializer);
        this.deserializer = deserializer;
    }

    /**
     * Set the pre-invokation filters created invokers should use.
     * @param preInvokeFilters preinvokation filters
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setPreInvokeFilters(List<? extends PreInvokeFilter> preInvokeFilters) {
        Validate.noNullElements(preInvokeFilters);
        this.preInvokeFilters = Collections.unmodifiableList(preInvokeFilters);
    }

    /**
     * Set the post-invokation filters created invokers should use.
     * @param postInvokeFilters postinvokation filters
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     */
    public void setPostInvokeFilters(List<? extends PostInvokeFilter> postInvokeFilters) {
        Validate.noNullElements(postInvokeFilters);
        this.postInvokeFilters = Collections.unmodifiableList(postInvokeFilters);
    }

    @Override
    public <T> Invoker<T> createInvoker(T object) {
        Validate.notNull(object);
        
        return new ReflectionInvoker<>(object, executor, serializer, deserializer, preInvokeFilters, postInvokeFilters);
    }
    
}
