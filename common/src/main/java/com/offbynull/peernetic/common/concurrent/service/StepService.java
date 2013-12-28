package com.offbynull.peernetic.common.concurrent.service;

public interface StepService {
    void setListener(ServiceListener listener);
    ServiceListener getListener();
    void onStart() throws Exception;
    boolean onStep() throws Exception;
    void onStop() throws Exception;
    void triggerStop();
}
