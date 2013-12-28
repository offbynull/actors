package com.offbynull.peernetic.common.concurrent.service;

public interface Service {
    void setListener(ServiceListener listener);
    ServiceListener getListener();
    void onStart() throws Exception;
    void onProcess() throws Exception;
    void onStop() throws Exception;
    void triggerStop();
}
