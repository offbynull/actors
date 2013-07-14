package com.offbynull.p2prpc.invoke;

import com.offbynull.p2prpc.Client;
import com.offbynull.p2prpc.invoke.Deserializer.DeserializerResult;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public final class Capturer<T> {
    private Class<T> cls;
    private Serializer serializer;
    private Deserializer deserializer;
    private List<Filter> inFilters;
    private List<Filter> outFilters;

    public Capturer(Class<T> cls) {
        this(cls,
                new XStreamBinarySerializerDeserializer(),
                new XStreamBinarySerializerDeserializer(),
                new Filter[] { new CompressionFilter() },
                new Filter[] { new CompressionFilter() });
    }
    
    public Capturer(Class<T> cls,
            Serializer serializer, Deserializer deserializer,
            Filter[] inFilters, Filter[] outFilters) {
        this.cls = cls;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.inFilters = new ArrayList<>(Arrays.asList(inFilters));
        this.outFilters = new ArrayList<>(Arrays.asList(outFilters));
    }
    
    public T createInstance(final CapturerCallback callback) {
        return  (T) Enhancer.create(cls, new MethodInterceptor() {
            @Override
            public Object intercept(Object obj, Method method, Object[] args,
                    MethodProxy proxy) throws Throwable {
                String name = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();

                InvokeData invokeData = new InvokeData(name, args, paramTypes);

                // Serialize and filter input
                byte[] inData;
                try {
                    inData = serializer.serializeMethodCall(invokeData);
                    
                    for (Filter filter : inFilters) {
                        inData = filter.unmodify(inData);
                    }
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    throw ioe;
                }
                
                // Call
                byte[] outData = callback.invokationTriggered(inData);
                
                // Filter and deserialize output
                DeserializerResult dr;
                try {
                    for (Filter filter : outFilters) {
                        outData = filter.unmodify(outData);
                    }
                    
                    dr = deserializer.deserialize(outData);
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    throw ioe;
                }

                if (dr.getType() == SerializationType.METHOD_RETURN) {
                    return dr.getResult();
                } else if (dr.getType() == SerializationType.METHOD_THROW) {
                    throw (Throwable) dr.getResult();
                }
                
                throw new IOException("Expected "
                        + SerializationType.METHOD_RETURN + " or "
                        + SerializationType.METHOD_THROW + " but found "
                        + dr);
            }
        });
    }
}
