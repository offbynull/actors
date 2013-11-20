package com.offbynull.p2prpc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public class RpcConfig<A> {
    
    private ExecutorService invokerExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    public ExecutorService getInvokerExecutorService() {
        return invokerExecutorService;
    }

    public void setInvokerExecutorService(ExecutorService invokerExecutorService) {
        Validate.notNull(invokerExecutorService);
        this.invokerExecutorService.shutdownNow();
        this.invokerExecutorService = invokerExecutorService;
    }
}
