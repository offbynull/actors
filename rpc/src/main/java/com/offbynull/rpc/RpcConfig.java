package com.offbynull.rpc;

import com.offbynull.rpc.transport.IncomingFilter;
import com.offbynull.rpc.transport.IncomingMessageListener;
import com.offbynull.rpc.transport.OutgoingFilter;
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

/**
 * RPC configuration.
 * @author Kasra F
 * @param <A> address type
 */
public final class RpcConfig<A> {
    
    private ExecutorService invokerExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    private Map<? extends Object, ? extends Object> extraInvokeInfo = Collections.emptyMap();
    private List<IncomingMessageListener<A>> preIncomingMessageListeners = Collections.emptyList();
    private List<IncomingMessageListener<A>> postIncomingMessageListeners = Collections.emptyList();
    private List<IncomingFilter<A>> incomingFilters = Collections.emptyList();
    private List<OutgoingFilter<A>> outgoingFilters = Collections.emptyList();

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

    public List<IncomingMessageListener<A>> getPreIncomingMessageListeners() {
        return preIncomingMessageListeners;
    }

    public void setPreIncomingMessageListeners(List<IncomingMessageListener<A>> preIncomingMessageListeners) {
        Validate.noNullElements(preIncomingMessageListeners);
        this.preIncomingMessageListeners = Collections.unmodifiableList(new ArrayList<>(preIncomingMessageListeners));
    }

    public List<IncomingMessageListener<A>> getPostIncomingMessageListeners() {
        return postIncomingMessageListeners;
    }

    public void setPostIncomingMessageListeners(List<IncomingMessageListener<A>> postIncomingMessageListeners) {
        Validate.noNullElements(postIncomingMessageListeners);
        this.postIncomingMessageListeners = Collections.unmodifiableList(new ArrayList<>(postIncomingMessageListeners));
    }

    public List<IncomingFilter<A>> getIncomingFilters() {
        return incomingFilters;
    }

    public void setIncomingFilters(List<IncomingFilter<A>> incomingFilters) {
        Validate.noNullElements(incomingFilters);
        this.incomingFilters = Collections.unmodifiableList(new ArrayList<>(incomingFilters));
    }

    public List<OutgoingFilter<A>> getOutgoingFilters() {
        return outgoingFilters;
    }

    public void setOutgoingFilters(List<OutgoingFilter<A>> outgoingFilters) {
        Validate.noNullElements(outgoingFilters);
        this.outgoingFilters = Collections.unmodifiableList(new ArrayList<>(outgoingFilters));
    }
    
}
