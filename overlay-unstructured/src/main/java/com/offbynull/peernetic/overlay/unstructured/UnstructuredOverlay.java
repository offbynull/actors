package com.offbynull.peernetic.overlay.unstructured;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

public final class UnstructuredOverlay<A> extends AbstractExecutionThreadService {
    private LinkManager<A> linkManager;

    @Override
    protected void run() throws Exception {
        while (true) {
            long timestamp = System.currentTimeMillis();
            long nextTimestamp = linkManager.process(timestamp);
            
            Thread.sleep(Math.max(1L, nextTimestamp - timestamp));
        }
    }
    
}
