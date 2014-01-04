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
package com.offbynull.peernetic.rpc.invoke.capturers.cglib;

import com.offbynull.peernetic.rpc.invoke.Capturer;
import com.offbynull.peernetic.rpc.invoke.CapturerHandler;
import com.offbynull.peernetic.rpc.invoke.CapturerUtils;
import com.offbynull.peernetic.rpc.invoke.Deserializer;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamSerializer;
import com.offbynull.peernetic.rpc.invoke.Deserializer.DeserializerResult;
import com.offbynull.peernetic.rpc.invoke.InvokeData;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamDeserializer;
import java.io.IOException;
import java.lang.reflect.Method;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.Validate;

/**
 * Provides the ability to proxy a non-final class or interface such that method invocations are processed by some external source.
 * Invocations are sent to the external source as a serialized byte array, and each invocation waits for the external source to give back
 * either a result to be returned by the invocation or a {@link Throwable} to be thrown from the invocation. {@link CapturerHandler} is the
 * processing mechanism.
 * @author Kasra Faghihi
 * @param <T> proxy type
 */
public final class CglibCapturer<T> implements Capturer<T> {
    private Class<T> cls;
    private Serializer serializer;
    private Deserializer deserializer;

    /**
     * Constructs a {@link Capturer} object with {@link XStreamSerializer} for serialization.
     * @param cls class type to proxy
     * @throws NullPointerException if any arguments are {@code null}
     */
    public CglibCapturer(Class<T> cls) {
        this(cls,
                new XStreamSerializer(),
                new XStreamDeserializer());
    }

    /**
     * Constructs a {@link Capturer} object.
     * @param cls class type to proxy
     * @param serializer serializer to use for invocation data
     * @param deserializer serializer to use for result data
     * @throws NullPointerException if any arguments are {@code null}
     */
    public CglibCapturer(Class<T> cls,
            Serializer serializer, Deserializer deserializer) {
        Validate.notNull(cls);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        
        this.cls = cls;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }
    
    /**
     * Creates a proxy object.
     * @param callback callback to notify when a method's been invoked on the returned proxy object
     * @return proxy object
     * @throws NullPointerException if any arguments are {@code null}
     */
    @Override
    public T createInstance(final CapturerHandler callback) {
        Validate.notNull(callback);
        
        return  (T) Enhancer.create(cls, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                String name = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();

                InvokeData invokeData = new InvokeData(name, args, paramTypes);

                // Serialize input
                byte[] inData;
                try {
                    inData = serializer.serializeMethodCall(invokeData);
                } catch (RuntimeException e) {
                    callback.invocationFailed(e);
                    throw e;
                }
                
                // Call
                byte[] outData = callback.invocationTriggered(inData);
                
                // Deserialize output
                DeserializerResult dr;
                try {
                    dr = deserializer.deserialize(outData);
                } catch (RuntimeException e) {
                    callback.invocationFailed(e);
                    throw e;
                }

                Object result = dr.getResult();
                switch (dr.getType()) {
                    case METHOD_RETURN:
                        try {
                            CapturerUtils.validateReturn(method, result);
                        } catch (RuntimeException e) {
                            callback.invocationFailed(e);
                            throw e;
                        }
                        
                        return result;
                    case METHOD_THROW: {
                        try {
                            CapturerUtils.validateThrowable(method, result);
                        } catch (RuntimeException e) {
                            callback.invocationFailed(e);
                            throw e;
                        }
                        
                        throw (Throwable) result;
                    }
                    default:
                        throw new IOException("Expected "
                                + SerializationType.METHOD_RETURN + " or "
                                + SerializationType.METHOD_THROW + " but found "
                                + dr);
                }
            }
        });
    }
}
