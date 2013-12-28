package com.offbynull.peernetic.common.concurrent.service;

public class StepServiceWrapper implements Service {
    private StepService stepService;
    private volatile boolean stop;

    public StepServiceWrapper(StepService stepService) {
        this.stepService = stepService;
    }

    @Override
    public void setListener(ServiceListener listener) {
        stepService.setListener(listener);
    }

    @Override
    public ServiceListener getListener() {
        return stepService.getListener();
    }

    @Override
    public void onStart() throws Exception {
        stepService.onStart();
    }

    @Override
    public void onProcess() throws Exception {
        while (true) {
            if (!stepService.onStep()) {
                break;
            }
        }
    }

    @Override
    public void onStop() throws Exception {
        stepService.onStop();
    }

    @Override
    public void triggerStop() {
        stepService.triggerStop();
    }
    
}
