package com.offbynull.peernetic.common.concurrent.lifecycle;

public class LifeCycleAdapter implements LifeCycle {
    private volatile LifeCycleListener listener;

    @Override
    public void setListener(LifeCycleListener listener) {
        this.listener = listener;
    }

    @Override
    public LifeCycleListener getListener() {
        return this.listener;
    }

    @Override
    public void onStart(Object ... init) throws Exception {
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
