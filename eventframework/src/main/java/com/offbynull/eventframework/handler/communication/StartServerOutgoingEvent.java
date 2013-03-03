package com.offbynull.eventframework.handler.communication;

import com.offbynull.eventframework.handler.DefaultTrackedOutgoingEvent;

public final class StartServerOutgoingEvent
        extends DefaultTrackedOutgoingEvent {
    
    private int port;

    public StartServerOutgoingEvent(int port, long trackedId) {
        super(trackedId);

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException();
        }
        
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
