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
import com.offbynull.peernetic.rpc.invoke.InvokeData;
import com.offbynull.peernetic.rpc.invoke.PreInvokeFilter;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import com.offbynull.peernetic.rpc.invoke.Invoker;
import com.offbynull.peernetic.rpc.invoke.InvokerListener;
import com.offbynull.peernetic.rpc.invoke.PostInvokeFilter;
import com.offbynull.peernetic.rpc.invoke.PostInvokeFilter.Result;
import com.offbynull.peernetic.rpc.invoke.PostInvokeFilter.ResultType;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import com.offbynull.peernetic.rpc.invoke.filters.sanity.AvoidObjectMethodsPreInvokeFilter;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamDeserializer;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamSerializer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

/**
 * Invokes methods on an object based on serialized data using Java reflections. Optionally allows the invocations to get executed through a
 * user-defined {@link ExecutorService}.
 * @param <T> type
 * @author Kasra Faghihi
 */
public final class ReflectionInvoker<T> implements Invoker<T> {

    private Object object;
    private ExecutorService executor;
    private Serializer serializer;
    private Deserializer deserializer;
    private List<PreInvokeFilter> preInvokeFilters;
    private List<PostInvokeFilter> postInvokeFilters;


    /**
     * Constructs an {@link ReflectionInvoker} object with {@link XStreamSerializer} / {@link XStreamDeserializer} for serialization and
     * {@link AvoidObjectMethodsPreInvokeFilter} as a filter.
     * @param object object to invoke on
     * @param executor executor to use for invocations
     * @throws NullPointerException if any argument is {@code null}
     */
    public ReflectionInvoker(T object, ExecutorService executor) {
        this(object, executor,
                new XStreamSerializer(),
                new XStreamDeserializer(),
                Arrays.asList(new AvoidObjectMethodsPreInvokeFilter()),
                Arrays.<PostInvokeFilter>asList());
    }
    
    /**
     * Constructs a {@link ReflectionInvoker} object.
     * @param object object to invoke on
     * @param executor executor to use for invocations
     * @param serializer serializer to use for invocation data
     * @param deserializer serializer to use for result data
     * @param preInvokeFilters pre invoke filters
     * @param postInvokeFilters post invoke filters
     * @throws NullPointerException if any arguments is {@code null}, or if any element within a collection is {@code null}
     */
    public ReflectionInvoker(T object, ExecutorService executor,
            Serializer serializer, Deserializer deserializer,
            List<? extends PreInvokeFilter> preInvokeFilters,
            List<? extends PostInvokeFilter> postInvokeFilters) {
        Validate.notNull(object);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        Validate.noNullElements(preInvokeFilters);
        
        this.object = object;
        this.executor = executor;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.preInvokeFilters = new ArrayList<>(preInvokeFilters);
        this.postInvokeFilters = new ArrayList<>(postInvokeFilters);
    }
    
    @Override
    public void invoke(final byte[] data, final InvokerListener callback) {
        invoke(data, callback, Collections.emptyMap());
    }

    @Override
    public void invoke(final byte[] data, final InvokerListener callback, Map<? extends Object, ? extends Object> info) {
        Validate.notNull(data);
        Validate.notNull(callback);
        Validate.notNull(info);
        
        final Map<Object, Object> sharedDataCopy = new HashMap<>(info);
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                byte[] inData = data;
                
                // Deserialize input
                InvokeData invokeData;
                try {
                    Deserializer.DeserializerResult dr =
                            deserializer.deserialize(inData);
                    
                    if (dr.getType() != SerializationType.METHOD_CALL) {
                        throw new IOException("Expected "
                                + SerializationType.METHOD_CALL + " but found"
                                + dr);
                    }
                    
                    invokeData = (InvokeData) dr.getResult();
                } catch (RuntimeException | IOException ioe) {
                    callback.invocationFailed(ioe);
                    return;
                }

                // Filter
                try {
                    for (PreInvokeFilter filter : preInvokeFilters) {
                        invokeData = filter.filter(invokeData);
                    }
                } catch (RuntimeException re) {
                    callback.invocationFailed(re);
                    return;
                }
                
                // Call
                Result result;
                try {
                    InvokeThreadInformation.setInvokeThreadInfo(sharedDataCopy);
                    
                    Object ret = MethodUtils.invokeMethod(object,
                            invokeData.getMethodName(),
                            invokeData.getArguments());
                    
                    result = new Result(ResultType.RETURN, ret);
                } catch (InvocationTargetException ex) {
                    result = new Result(ResultType.THROW, ex.getCause());
                } catch (RuntimeException | NoSuchMethodException | IllegalAccessException ex) {
                    // throws npe if method expects primitves
                    callback.invocationFailed(ex);
                    return;
                } finally {
                    InvokeThreadInformation.removeInvokeThreadInfo();
                }

                // Filter
                try {
                    for (PostInvokeFilter filter : postInvokeFilters) {
                        result = filter.filter(result);
                    }
                } catch (RuntimeException re) {
                    callback.invocationFailed(re);
                    return;
                }
                
                // Serialize output
                byte[] outData;
                try {
                    switch (result.getType()) {
                        case RETURN:
                            outData = serializer.serializeMethodReturn(result.getResult());
                            break;
                        case THROW:
                            outData = serializer.serializeMethodThrow((Exception) result.getResult());
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                } catch (RuntimeException re) {
                    callback.invocationFailed(re);
                    return;
                }
                
                // Send
                try {
                    callback.invocationFinised(outData);
                } catch (RuntimeException re) { // NOPMD
                    // don't bother calling invocationFailed here, we've already attempted to end the invocation by calling finish
                }
            }
        };
        
        try {
            executor.execute(r);
        } catch (RuntimeException ree) {
            callback.invocationFailed(ree);
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.interrupted(); // just in case this is interrupted exception
            throw new IOException(ex);
        } catch (RuntimeException re) {
            throw new IOException(re);
        }
    }
}
