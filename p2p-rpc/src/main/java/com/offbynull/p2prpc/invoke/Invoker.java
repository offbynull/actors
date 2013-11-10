package com.offbynull.p2prpc.invoke;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.MethodUtils;

public final class Invoker implements Closeable {

    private Object object;
    private ExecutorService executor;
    private Serializer serializer;
    private Deserializer deserializer;
    private List<Filter> inFilters;
    private List<Filter> outFilters;

    public Invoker(Object object) {
        this(object, null);
    }

    public Invoker(Object object, int threadSize) {
        this(object, Executors.newFixedThreadPool(threadSize));
    }

    public Invoker(Object object, ExecutorService executor) {
        this(object, executor,
                new XStreamBinarySerializerDeserializer(),
                new XStreamBinarySerializerDeserializer(),
                new Filter[] { new CompressionFilter() },
                new Filter[] { new CompressionFilter() });
    }
    
    public Invoker(Object object, ExecutorService executor,
            Serializer serializer, Deserializer deserializer,
            Filter[] inFilters, Filter[] outFilters) {
        Validate.notNull(object);
        Validate.notNull(serializer);
        Validate.notNull(deserializer);
        Validate.noNullElements(inFilters);
        Validate.noNullElements(outFilters);
        
        this.object = object;
        this.executor = executor;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.inFilters = new ArrayList<>(Arrays.asList(inFilters));
        this.outFilters = new ArrayList<>(Arrays.asList(outFilters));
    }
    
    public void invoke(final byte[] data, final InvokerCallback callback) {
        Validate.notNull(data);
        Validate.notNull(callback);
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                byte[] inData = data;
                
                // Filter and deserialize input
                InvokeData invokeData;
                try {
                    for (Filter filter : inFilters) {
                        inData = filter.unmodify(inData);
                    }
                    
                    Deserializer.DeserializerResult dr =
                            deserializer.deserialize(data);
                    
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
                }
                
                // Filter output
                try {
                    for (Filter filter : outFilters) {
                        outData = filter.unmodify(outData);
                    }
                } catch (IOException ioe) {
                    callback.invokationFailed(ioe);
                    return;
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
        } catch (Exception ex) {
            Thread.interrupted(); // just in case this is interrupted exception
            throw new IOException(ex);
        }
    }
}
