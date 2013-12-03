package com.offbynull.rpc.invoke;

import com.offbynull.rpc.invoke.Deserializer.DeserializerResult;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

/**
 * Provides the ability to proxy a asynchronous interface such that method invokations are processed by some external source. This class
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
 * Async interface for this class ({@link AsyncCapturer}):
 * <pre>
  * public void MyServiceClass {
 *    void performFunction(AsyncResultListener<String> result, int id);
 * }
 * </pre>
 * @author Kasra F
 * @param <T> proxy type
 * @param <AT> proxy async type
 */
public final class AsyncCapturer<T, AT> {
    private Class<T> cls;
    private Class<AT> asyncCls;
    private Map<Method, Method> methodMap;
    private Serializer serializer;
    private Deserializer deserializer;

    /**
     * Constructs a {@link AsyncCapturer} object with {@link XStreamBinarySerializerDeserializer} for serialization.
     * @param cls proxy type
     * @param asyncCls proxy async type
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if methods declared in {@code asyncCls} don't have an equivalent in {@code cls}
     */
    public AsyncCapturer(Class<T> cls, Class<AT> asyncCls) {
        this(cls, asyncCls,
                new XStreamBinarySerializerDeserializer(),
                new XStreamBinarySerializerDeserializer());
    }

    /**
     * Constructs a {@link Capturer} object.
     * @param cls proxy type
     * @param asyncCls proxy async type
     * @param serializer serializer to use for invokation data
     * @param deserializer serializer to use for result data
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if methods declared in {@code asyncCls} don't have an equivalent in {@code cls}
     */
    public AsyncCapturer(Class<T> cls, Class<AT> asyncCls,
            Serializer serializer, Deserializer deserializer) {
        Validate.notNull(cls);
        Validate.notNull(asyncCls);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        
        Validate.isTrue(asyncCls.isInterface());
        
        methodMap = new HashMap<>();
        
        Method[] asyncMethods = asyncCls.getDeclaredMethods();
        for (Method asyncMethod : asyncMethods) {
            Validate.isTrue(Modifier.isPublic(asyncMethod.getModifiers()), "All asyncCls methods must be public");
            Validate.isTrue(asyncMethod.getReturnType() == Void.TYPE, "All asyncCls methods must return void");
            
            Class<?>[] asyncParamTypes = asyncMethod.getParameterTypes();
            
            Validate.isTrue(asyncParamTypes.length > 0 && asyncParamTypes[0] == AsyncResultListener.class,
                    "All asyncCls methods must have " + AsyncResultListener.class.getSimpleName() + " as first method");
            
            String methodName = asyncMethod.getName();
            Class<?>[] relevantAsyncParamTypes = Arrays.copyOfRange(asyncParamTypes, 1, asyncParamTypes.length);

            Method syncMethod;
            try {
                syncMethod = cls.getMethod(methodName, relevantAsyncParamTypes);
            } catch (NoSuchMethodException nsme) {
                throw new IllegalArgumentException("Method " + methodName + " not found");
            }
            
            methodMap.put(asyncMethod, syncMethod);
        }
        
        
        this.cls = cls;
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
    public AT createInstance(final AsyncCapturerHandler callback) {
        Validate.notNull(callback);
        
        return  (AT) Enhancer.create(asyncCls, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, final Method method, Object[] args, MethodProxy proxy) throws Throwable {
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
                            case METHOD_RETURN: {
                                Object ret = dr.getResult();
                                
                                Method syncMethod = methodMap.get(method);
                                Class<?> returnType = syncMethod.getReturnType();
                                
                                if (ret != null) {
                                    if (returnType == Void.TYPE) {
                                        resultListener.invokationFailed("Return non-null for void type");
                                        break;
                                    } else if (!ClassUtils.isAssignable(ret.getClass(), returnType)) {
                                        resultListener.invokationFailed("Return non-matching class type");
                                        break;
                                    }
                                } else {
                                    if (returnType.isPrimitive() && returnType != Void.TYPE) {
                                        resultListener.invokationFailed("Return null for primitive type");
                                        break;
                                    }
                                }
                                
                                resultListener.invokationReturned(dr.getResult());
                                break;
                            }
                            case METHOD_THROW: {
                                Object ret = dr.getResult();
                                
                                if (!(ret instanceof Throwable)) {
                                    resultListener.invokationFailed("Throw non-throwable type");
                                } else {
                                    resultListener.invokationThrew((Throwable) ret);
                                }
                                break;
                            }
                            default: {
                                resultListener.invokationFailed("Expected "
                                        + SerializationType.METHOD_RETURN + " or "
                                        + SerializationType.METHOD_THROW + " but found "
                                        + dr);
                                break;
                            }
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
