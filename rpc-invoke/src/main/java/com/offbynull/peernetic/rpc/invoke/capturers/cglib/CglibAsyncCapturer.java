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

import com.offbynull.peernetic.rpc.invoke.AsyncCapturer;
import com.offbynull.peernetic.rpc.invoke.AsyncCapturerHandler;
import com.offbynull.peernetic.rpc.invoke.AsyncCapturerHandlerCallback;
import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;
import com.offbynull.peernetic.rpc.invoke.CapturerUtils;
import com.offbynull.peernetic.rpc.invoke.Deserializer;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamSerializer;
import com.offbynull.peernetic.rpc.invoke.Deserializer.DeserializerResult;
import com.offbynull.peernetic.rpc.invoke.InvokeData;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import com.offbynull.peernetic.rpc.invoke.serializers.xstream.XStreamDeserializer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.Validate;

/**
 * Provides the ability to proxy a asynchronous interface such that method invocations are processed by some external source. This class
 * essentially does what {@link Capturer} does, but does so using an asynchronous interface. That is, this class expects to proxy an
 * interface where the method signatures of the interface resemble that of a non-final class/interface that you would pass in to
 * {@link Capturer}. The difference between the method signatures are that..
 * <ol>
 * <li>Return type must be void.</li>
 * <li>An extra parameter of type {@link AsyncResultListener} must be added in as the first parameter.</li>
 * </ol>
 * The return value / thrown exception will be passed back to the AsyncResultListener object passed in to the first argument.
 * <p/>
 * Example...
 * <p/>
 * Original interface for {@link Capturer}:
 * <pre>
 * public void MyServiceClass {
 *    String performFunction(int id);
 * }
 * </pre>
 * <p/>
 * Async interface for this class ({@link CglibAsyncCapturer}):
 * <pre>
  * public void MyServiceClass {
 *    void performFunction(AsyncResultListener<String> result, int id);
 * }
 * </pre>
 * @author Kasra Faghihi
 * @param <T> proxy type
 * @param <AT> proxy async type
 */
public final class CglibAsyncCapturer<T, AT> implements AsyncCapturer<T, AT> {
//    private Class<T> cls;
    private Class<AT> asyncCls;
    private Map<Method, Method> methodMap;
    private Serializer serializer;
    private Deserializer deserializer;

    /**
     * Constructs a {@link AsyncCapturer} object with {@link XStreamSerializer} for serialization.
     * @param cls proxy type
     * @param asyncCls proxy async type
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if methods declared in {@code asyncCls} don't have an equivalent in {@code cls}
     */
    public CglibAsyncCapturer(Class<T> cls, Class<AT> asyncCls) {
        this(cls, asyncCls,
                new XStreamSerializer(),
                new XStreamDeserializer());
    }

    /**
     * Constructs a {@link Capturer} object.
     * @param cls proxy type
     * @param asyncCls proxy async type
     * @param serializer serializer to use for invocation data
     * @param deserializer serializer to use for result data
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if methods declared in {@code asyncCls} don't have an equivalent in {@code cls}
     */
    public CglibAsyncCapturer(Class<T> cls, Class<AT> asyncCls,
            Serializer serializer, Deserializer deserializer) {
        Validate.notNull(cls);
        Validate.notNull(asyncCls);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        
        Validate.isTrue(asyncCls.isInterface());
        
        methodMap = CapturerUtils.mapAsyncInterfaceToNormalClass(cls, asyncCls);
        
        
//        this.cls = cls;
        this.asyncCls = asyncCls;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }
    
    /**
     * Creates an async proxy object.
     * @param callback callback to notify when a method's been invoked on the returned proxy object
     * @return proxy object
     * @throws NullPointerException if any arguments are {@code null}
     */
    @Override
    public AT createInstance(final AsyncCapturerHandler callback) {
        Validate.notNull(callback);
        
        return  (AT) Enhancer.create(asyncCls, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, final Method method, Object[] args, MethodProxy proxy) throws Throwable {
                // Method already validated as being correct through mapping method in constructor
                
                try {
                    Validate.notNull(args[0], "First argument must be non-null");
                } catch (NullPointerException e) {
                    callback.invocationFailed(e);
                    throw e;
                }
                
                final AsyncResultListener<Object> resultListener = (AsyncResultListener<Object>) args[0];

                Class<?>[] paramTypes = method.getParameterTypes();
                Class<?>[] sanitizedParamTypes = Arrays.copyOfRange(paramTypes, 1, args.length);
                Object[] sanitizedArgs = Arrays.copyOfRange(args, 1, args.length);
                
                String name = method.getName();
                InvokeData invokeData = new InvokeData(name, sanitizedArgs, sanitizedParamTypes);

                // Serialize input
                byte[] inData;
                try {
                    inData = serializer.serializeMethodCall(invokeData);
                } catch (RuntimeException e) {
                    callback.invocationFailed(e);
                    throw e;
                }
                
                // Call
                callback.invocationTriggered(inData, new AsyncCapturerHandlerCallback() {

                    @Override
                    public void responseArrived(byte[] outData) {
                        // Deserialize output
                        DeserializerResult dr;
                        try {
                            dr = deserializer.deserialize(outData);
                        } catch (RuntimeException e) {
                            callback.invocationFailed(e);
                            throw e;
                        }

                        switch (dr.getType()) {
                            case METHOD_RETURN: {
                                Object ret = dr.getResult();
                                Method syncMethod = methodMap.get(method);
                                
                                try {
                                    CapturerUtils.validateReturn(syncMethod, ret);
                                } catch (RuntimeException re) {
                                    resultListener.invocationFailed(re);
                                    throw re;
                                }
                                
                                resultListener.invocationReturned(dr.getResult());
                                break;
                            }
                            case METHOD_THROW: {
                                Object ret = dr.getResult();
                                Method syncMethod = methodMap.get(method);
                                
                                try {
                                    CapturerUtils.validateThrowable(syncMethod, ret);
                                } catch (RuntimeException re) {
                                    resultListener.invocationFailed(re);
                                    throw re;
                                }
                                
                                resultListener.invocationThrew((Throwable) ret);
                                break;
                            }
                            default: {
                                try {
                                    throw new RuntimeException("Expected "
                                            + SerializationType.METHOD_RETURN + " or "
                                            + SerializationType.METHOD_THROW + " but found "
                                            + dr);
                                } catch (RuntimeException e) {
                                    resultListener.invocationFailed(e);
                                    throw e;
                                }
                            }
                        }
                    }

                    @Override
                    public void responseFailed(Throwable err) {
                        resultListener.invocationFailed(err);                        
                    }
                });
                
                return null;
            }
        });
    }
}
