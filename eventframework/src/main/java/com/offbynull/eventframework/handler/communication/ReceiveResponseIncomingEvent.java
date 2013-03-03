package com.offbynull.eventframework.handler.communication;

import com.offbynull.eventframework.handler.DefaultSuccessIncomingEvent;

public final class ReceiveResponseIncomingEvent
        extends DefaultSuccessIncomingEvent{
    private Response response;

    public ReceiveResponseIncomingEvent(Response response, long trackedId) {
        super(trackedId);
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
    
}
