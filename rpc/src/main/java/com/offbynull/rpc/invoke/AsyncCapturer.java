package com.offbynull.rpc.invoke;

import com.offbynull.rpc.invoke.Deserializer.DeserializerResult;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
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
        
        methodMap = CapturerUtils.mapAsyncInterfaceToNormalClass(cls, asyncCls);
        
        
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
                // Method already validated as being correct through mapping method in constructor
                
                try {
                    Validate.notNull(args[0], "First argument must be non-null");
                } catch (NullPointerException e) {
                    callback.invokationFailed(e);
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
                                
                                try {
                                    CapturerUtils.validateReturn(syncMethod, ret);
                                } catch (RuntimeException re) {
                                    resultListener.invokationFailed(re);
                                    throw re;
                                }
                                
                                resultListener.invokationReturned(dr.getResult());
                                break;
                            }
                            case METHOD_THROW: {
                                Object ret = dr.getResult();
                                Method syncMethod = methodMap.get(method);
                                
                                try {
                                    CapturerUtils.validateThrowable(syncMethod, ret);
                                } catch (RuntimeException re) {
                                    resultListener.invokationFailed(re);
                                    throw re;
                                }
                                
                                resultListener.invokationThrew((Throwable) ret);
                                break;
                            }
                            default: {
                                RuntimeException re = new RuntimeException("Expected "
                                        + SerializationType.METHOD_RETURN + " or "
                                        + SerializationType.METHOD_THROW + " but found "
                                        + dr);
                                
                                try {
                                    throw re;
                                } catch (RuntimeException e) {
                                    resultListener.invokationFailed(e);
                                    throw e;
                                }
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
