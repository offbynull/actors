package com.offbynull.peernetic.common.concurrent.service;

import com.offbynull.peernetic.common.concurrent.pump.PumpReader;
import java.util.Iterator;

public abstract class PumpService<T> extends Service {

    private PumpReader<T> pumpReader;
    
    public PumpService(String name, boolean daemon) {
        super(name, daemon);
    }

    public PumpService(String name, boolean daemon, ServiceStopTrigger stopTrigger) {
        super(name, daemon, stopTrigger);
    }

    @Override
    protected final void onProcess() throws Exception {
        long waitUntil = Long.MAX_VALUE;
        
        while (true) {
            Iterator<T> messages = pumpReader.pull(waitUntil);

            long preStepTime = System.currentTimeMillis();
            long nextStepTime = onStep(preStepTime, messages);
            
            if (nextStepTime < 0L) {
                return;
            }
            
            long postStepTime = System.currentTimeMillis();
            
            waitUntil = Math.max(nextStepTime - postStepTime, 0L);
        }
    }
    
    protected abstract long onStep(long timestamp, Iterator<T> messages);
    
}
