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
import com.offbynull.peernetic.rpc.invoke.InvokeFilter;
import com.offbynull.peernetic.rpc.invoke.InvokeThreadInformation;
import com.offbynull.peernetic.rpc.invoke.Invoker;
import com.offbynull.peernetic.rpc.invoke.InvokerListener;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import com.offbynull.peernetic.rpc.invoke.filters.sanity.AvoidObjectInvokeFilter;
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
 * Invokes methods on an object based on serialized data using Java reflections. Optionally allows the invokations to get executed through a
 * user-defined {@link ExecutorService}.
 * @author Kasra Faghihi
 */
public final class ReflectionInvoker implements Invoker {

    private Object object;
    private ExecutorService executor;
    private Serializer serializer;
    private Deserializer deserializer;
    private List<InvokeFilter> filters;


    /**
     * Constructs an {@link ReflectionInvoker} object with {@link XStreamSerializer} / {@link XStreamDeserializer} for serialization and
     * {@link AvoidObjectInvokeFilter} as a filter.
     * @param object object to invoke on
     * @param executor executor to use for invokations
     * @throws NullPointerException if {@code object} is {@code null}
     */
    public ReflectionInvoker(Object object, ExecutorService executor) {
        this(object, executor,
                new XStreamSerializer(),
                new XStreamDeserializer(),
                new AvoidObjectInvokeFilter());
    }
    
    /**
     * Constructs a {@link ReflectionInvoker} object.
     * @param object object to invoke on
     * @param executor executor to use for invokations
     * @param serializer serializer to use for invokation data
     * @param deserializer serializer to use for result data
     * @param filters invoke filters
     * @throws NullPointerException if any arguments other than {@code executor} are {@code null}, or if any collection element is
     * {@code null}
     */
    public ReflectionInvoker(Object object, ExecutorService executor,
            Serializer serializer, Deserializer deserializer, InvokeFilter ... filters) {
        Validate.notNull(object);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        Validate.noNullElements(filters);
        
        this.object = object;
        this.executor = executor;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.filters = new ArrayList<>(Arrays.asList(filters));
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
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    return;
                }

                // Filter
                for (InvokeFilter filter : filters) {
                    invokeData = filter.filter(invokeData);
                }
                
                // Set shared data map
                InvokeThreadInformation.setInvokeThreadInfo(sharedDataCopy);
                
                // Call and serialize
                byte[] outData;
                try {
                    Object ret = MethodUtils.invokeMethod(object,
                            invokeData.getMethodName(),
                            invokeData.getArguments());
                    
                    outData = serializer.serializeMethodReturn(ret);
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    callback.invokationFailed(ex);
                    return;
                } catch (InvocationTargetException ex) {
                    outData = serializer.serializeMethodThrow(ex.getCause());
                } catch (NullPointerException npe) {
                     // throws npe if method expects primitves
                    outData = serializer.serializeMethodThrow(npe);
                } finally {
                    InvokeThreadInformation.removeInvokeThreadInfo();
                }
                
                // Send
                callback.invokationFinised(outData);
            }
        };
        
        if (executor == null) {
            r.run();
        } else {
            executor.execute(r);
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
