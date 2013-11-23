package com.offbynull.p2prpc;

import com.offbynull.p2prpc.transport.IncomingMessageListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public final class RpcConfig<A> {
    
    private ExecutorService invokerExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    private Map<? extends Object, ? extends Object> extraInvokeInfo = Collections.emptyMap();
    private List<IncomingMessageListener<A>> preIncomingMessageFilters = Collections.emptyList();
    private List<IncomingMessageListener<A>> postIncomingMessageFilters = Collections.emptyList();

    public ExecutorService getInvokerExecutorService() {
        return invokerExecutorService;
    }

    public void setInvokerExecutorService(ExecutorService invokerExecutorService) {
        Validate.notNull(invokerExecutorService);
        this.invokerExecutorService.shutdownNow();
        this.invokerExecutorService = invokerExecutorService;
    }

    public Map<? extends Object, ? extends Object> getExtraInvokeInfo() {
        return extraInvokeInfo;
    }

    public void setExtraInvokeInfo(Map<? extends Object, ? extends Object> invokeDataMap) {
        Validate.notNull(invokeDataMap);
        for (RpcInvokeKeys key : RpcInvokeKeys.values()) {
            Validate.isTrue(!invokeDataMap.keySet().contains(key));
        }
        
        this.extraInvokeInfo = Collections.unmodifiableMap(new HashMap<>(invokeDataMap));
    }

    public List<IncomingMessageListener<A>> getPreIncomingMessageFilters() {
        return preIncomingMessageFilters;
    }

    public void setPreIncomingMessageFilters(List<IncomingMessageListener<A>> preIncomingMessageFilters) {
        Validate.noNullElements(preIncomingMessageFilters);
        this.preIncomingMessageFilters = Collections.unmodifiableList(new ArrayList<>(preIncomingMessageFilters));
    }

    public List<IncomingMessageListener<A>> getPostIncomingMessageFilters() {
        return postIncomingMessageFilters;
    }

    public void setPostIncomingMessageFilters(List<IncomingMessageListener<A>> postIncomingMessageFilters) {
        Validate.noNullElements(postIncomingMessageFilters);
        this.postIncomingMessageFilters = Collections.unmodifiableList(new ArrayList<>(postIncomingMessageFilters));
    }
    
}
