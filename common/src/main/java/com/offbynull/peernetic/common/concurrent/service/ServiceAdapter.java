package com.offbynull.peernetic.common.concurrent.service;

public class ServiceAdapter implements Service {
    private volatile ServiceListener listener;

    @Override
    public void setListener(ServiceListener listener) {
        this.listener = listener;
    }

    @Override
    public ServiceListener getListener() {
        return this.listener;
    }

    @Override
    public void onStart() throws Exception {
    }

    @Override
    public void onProcess() throws Exception {
    }

    @Override
    public void onStop() throws Exception {
    }

    @Override
    public void triggerStop() {
        // does nothing by default
    }
}
