package com.offbynull.peernetic.common.concurrent.lifecycle;

import java.util.concurrent.Callable;

public final class LifeCycleCallable implements Callable<Void> {

    private LifeCycle service;

    @Override
    public Void call() throws Exception {
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

        return null;
    }
}
