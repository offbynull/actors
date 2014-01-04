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
import com.offbynull.peernetic.rpc.invoke.serializers.java.JavaDeserializer;
import com.offbynull.peernetic.rpc.invoke.serializers.java.JavaSerializer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public final class ReflectionInvokerFactory implements InvokerFactory {
    private ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private Serializer serializer = new JavaSerializer();
    private Deserializer deserializer = new JavaDeserializer();
    private List<PreInvokeFilter> preInvokeFilters = Collections.emptyList();
    private List<PostInvokeFilter> postInvokeFilters = Collections.emptyList();

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        Validate.notNull(executor);
        Validate.isTrue(!executor.isShutdown());
        Validate.isTrue(!executor.isTerminated());
        this.executor = executor;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public void setSerializer(Serializer serializer) {
        Validate.notNull(serializer);
        this.serializer = serializer;
    }

    public Deserializer getDeserializer() {
        return deserializer;
    }

    public void setDeserializer(Deserializer deserializer) {
        Validate.notNull(deserializer);
        this.deserializer = deserializer;
    }

    public List<PreInvokeFilter> getPreInvokeFilters() {
        return preInvokeFilters;
    }

    public void setPreInvokeFilters(List<PreInvokeFilter> preInvokeFilters) {
        Validate.noNullElements(preInvokeFilters);
        this.preInvokeFilters = Collections.unmodifiableList(preInvokeFilters);
    }

    public List<PostInvokeFilter> getPostInvokeFilters() {
        return postInvokeFilters;
    }

    public void setPostInvokeFilters(List<PostInvokeFilter> postInvokeFilters) {
        Validate.noNullElements(postInvokeFilters);
        this.postInvokeFilters = Collections.unmodifiableList(postInvokeFilters);
    }

    @Override
    public <T> Invoker<T> createInvoker(T object) {
        Validate.notNull(object);
        
        return new ReflectionInvoker<>(object, executor, serializer, deserializer, preInvokeFilters, postInvokeFilters);
    }
    
}
