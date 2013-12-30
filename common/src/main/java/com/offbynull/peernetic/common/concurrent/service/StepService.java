package com.offbynull.peernetic.common.concurrent.service;

public class StepService extends Service {

    @Override
    protected final void onProcess() throws Exception {
    }

    protected void onStepStart() {
    }
    
    protected long onStep(long timestamp, Object message) {
        return 0L;
    }
    
    protected void onStepStop() {
        
    }
}
