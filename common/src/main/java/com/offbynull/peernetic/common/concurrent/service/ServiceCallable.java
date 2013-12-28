package com.offbynull.peernetic.common.concurrent.service;

import java.util.concurrent.Callable;

public final class ServiceCallable implements Callable<Void> {

    private Service service;

    @Override
    public Void call() throws Exception {
        ServiceListener listener = null;
        try {
            listener = service.getListener();

            if (listener != null) {
                listener.stateChanged(service, ServiceState.STARTING);
            }
            service.onStart();

            if (listener != null) {
                listener.stateChanged(service, ServiceState.PROCESSING);
            }
            service.onProcess();
        } catch (Exception t) {
            if (listener != null) {
                listener.stateChanged(service, ServiceState.FAILED);
            }
            throw new RuntimeException("Service error", t);
        } finally {
            try {
                if (listener != null) {
                    listener.stateChanged(service, ServiceState.STOPPING);
                }
                service.onStop();
            } catch (Exception t) {
                // do nothing
            }

            if (listener != null) {
                listener.stateChanged(service, ServiceState.FINISHED);
            }
        }

        return null;
    }
}
