package com.offbynull.p2prpc;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.reflect.MethodUtils;

public final class Invoker implements Closeable {

    private ExecutorService executor;
    private Object object;

    public Invoker(ExecutorService executor, Object object) {
        this.executor = executor;
        this.object = object;
    }

    public void invoke(final InvokeData data, final InvokerCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object ret = MethodUtils.invokeMethod(object,
                            data.getMethodName(),
                            data.getArguments());
                    
                    callback.methodReturned(ret);
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    callback.invokationErrored(ex);
                } catch (InvocationTargetException ex) {
                    callback.methodErrored(ex.getCause());
                }
            }
        });
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
