package com.offbynull.rpc.invoke;

import com.offbynull.rpc.invoke.Deserializer.DeserializerResult;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

public final class AsyncCapturer<T, AT> {
    private Class<T> cls;
    private Class<AT> asyncCls;
    private Serializer serializer;
    private Deserializer deserializer;

    public AsyncCapturer(Class<T> cls, Class<AT> asyncCls) {
        this(cls, asyncCls,
                new XStreamBinarySerializerDeserializer(),
                new XStreamBinarySerializerDeserializer());
    }

    public AsyncCapturer(Class<T> cls, Class<AT> asyncCls,
            Serializer serializer, Deserializer deserializer) {
        Validate.notNull(cls);
        Validate.notNull(asyncCls);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        
        Validate.isTrue(asyncCls.isInterface());
        
        Method[] asyncMethods = asyncCls.getDeclaredMethods();
        for (Method asyncMethod : asyncMethods) {
            Validate.isTrue(Modifier.isPublic(asyncMethod.getModifiers()), "All asyncCls methods must be public");
            Validate.isTrue(asyncMethod.getReturnType() == Void.TYPE, "All asyncCls methods must return void");
            
            Class<?>[] asyncParamTypes = asyncMethod.getParameterTypes();
            
            Validate.isTrue(asyncParamTypes.length > 0 && asyncParamTypes[0] == AsyncResultListener.class,
                    "All asyncCls methods must have " + AsyncResultListener.class.getSimpleName() + " as first method");
            
            String methodName = asyncMethod.getName();
            Class<?>[] relevantAsyncParamTypes = Arrays.copyOfRange(asyncParamTypes, 1, asyncParamTypes.length);

            try {
                cls.getMethod(methodName, relevantAsyncParamTypes);
            } catch (NoSuchMethodException nsme) {
                throw new IllegalArgumentException("Method " + methodName + " not found");
            }
        }
        
        
        this.cls = cls;
        this.asyncCls = asyncCls;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }
    
    public AT createInstance(final AsyncCapturerHandler callback) {
        Validate.notNull(callback);
        
        return  (AT) Enhancer.create(asyncCls, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                if (method.getReturnType() != Void.TYPE) {
                    try {
                        throw new RuntimeException("Method return type must be void");
                    } catch (RuntimeException e) {
                        callback.invokationFailed(e);
                        throw e;
                    }
                }
                
                String name = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();
                
                if (paramTypes.length == 0 || paramTypes[0] != AsyncResultListener.class) {
                    try {
                        throw new RuntimeException("First parameter must be " + AsyncResultListener.class.getSimpleName());
                    } catch (RuntimeException e) {
                        callback.invokationFailed(e);
                        throw e;
                    }
                }
                
                if (args[0] == null) {
                    try {
                        throw new NullPointerException("First argument must be non-null");
                    } catch (NullPointerException e) {
                        callback.invokationFailed(e);
                        throw e;
                    }
                }
                
                final AsyncResultListener<Object> resultListener = (AsyncResultListener<Object>) args[0];

                Class<?>[] sanitizedParamTypes = Arrays.copyOfRange(paramTypes, 1, args.length);
                Object[] sanitizedArgs = Arrays.copyOfRange(args, 1, args.length);
                
                InvokeData invokeData = new InvokeData(name, sanitizedArgs, sanitizedParamTypes);

                // Serialize input
                byte[] inData;
                try {
                    inData = serializer.serializeMethodCall(invokeData);
                } catch (RuntimeException e) {
                    callback.invokationFailed(e);
                    throw e;
                }
                
                // Call
                callback.invokationTriggered(inData, new AsyncCapturerHandlerCallback() {

                    @Override
                    public void responseArrived(byte[] outData) {
                        // Deserialize output
                        DeserializerResult dr;
                        try {
                            dr = deserializer.deserialize(outData);
                        } catch (RuntimeException e) {
                            callback.invokationFailed(e);
                            throw e;
                        }

                        switch (dr.getType()) {
                            case METHOD_RETURN:
                                resultListener.invokationReturned(dr.getResult());
                                break;
                            case METHOD_THROW:
                                resultListener.invokationThrew((Throwable) dr.getResult());
                                break;
                            default:
                                resultListener.invokationFailed("Expected "
                                        + SerializationType.METHOD_RETURN + " or "
                                        + SerializationType.METHOD_THROW + " but found "
                                        + dr);
                                break;
                        }
                    }

                    @Override
                    public void responseFailed(Throwable err) {
                        resultListener.invokationFailed(err);                        
                    }
                });
                
                return null;
            }
        });
    }
}
