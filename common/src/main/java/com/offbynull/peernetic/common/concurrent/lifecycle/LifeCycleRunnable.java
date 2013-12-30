package com.offbynull.peernetic.common.concurrent.lifecycle;

public final class LifeCycleRunnable implements Runnable {

    private LifeCycle service;

    public LifeCycleRunnable(LifeCycle service) {
        if (service == null) {
            throw new NullPointerException();
        }

        this.service = service;
    }

    @Override
    public void run() {
        LifeCycleListener listener = null;
        try {
            listener = service.getListener();

            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.STARTING);
            }
            service.onStart();

            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.PROCESSING);
            }
            service.onProcess();
        } catch (Exception t) {
            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.FAILED);
            }
            throw new RuntimeException("Service error", t);
        } finally {
            try {
                if (listener != null) {
                    listener.stateChanged(service, LifeCycleState.STOPPING);
                }
                service.onStop();
            } catch (Exception t) {
                // do nothing
            }
            
            if (listener != null) {
                listener.stateChanged(service, LifeCycleState.FINISHED);
            }
        }
    }
}
