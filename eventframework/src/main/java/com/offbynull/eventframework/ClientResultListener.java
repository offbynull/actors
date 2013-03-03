package com.offbynull.eventframework;

public interface ClientResultListener {
    void processorFinished(long timestamp, Object result);
    void processorErrored(long timestamp, String description);
    void processorException(Throwable throwable);
}
