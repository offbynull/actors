package com.offbynull.peernetic.common.concurrent.service;

public final class ServiceRunnable implements Runnable {

    private Service service;

    public ServiceRunnable(Service service) {
        if (service == null) {
            throw new NullPointerException();
        }

        this.service = service;
    }

    @Override
    public void run() {
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
    }
}
