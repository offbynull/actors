package com.offbynull.peernetic.eventframework.handler.communication;

import com.offbynull.peernetic.eventframework.event.DefaultTrackedOutgoingEvent;

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
