package com.offbynull.eventframework.network.impl.simpletcp;

import com.offbynull.eventframework.network.impl.message.Request;
import com.offbynull.peernetic.eventframework.event.DefaultTrackedIncomingEvent;

public final class ReceiveMessageIncomingEvent
        extends DefaultTrackedIncomingEvent {
    private Request request;
    private String remoteAddress;
    private int serverPort;
    private long pendingId;

    public ReceiveMessageIncomingEvent(Request request, String remoteAddress,
            int serverPort, long pendingId, long trackedId) {
        super(trackedId);

        if (remoteAddress == null) {
            throw new NullPointerException();
        }
        
        if (serverPort < 1 || serverPort > 65535) {
            throw new IllegalArgumentException();
        }
        
        this.request = request;
        this.remoteAddress = remoteAddress;
        this.serverPort = serverPort;
        this.pendingId = pendingId;
    }

    public Request getRequest() {
        return request;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public long getPendingId() {
        return pendingId;
    }
    
    public int getServerPort() {
        return serverPort;
    }
}
