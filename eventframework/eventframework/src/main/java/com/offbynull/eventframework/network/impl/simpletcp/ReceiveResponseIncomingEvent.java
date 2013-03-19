package com.offbynull.eventframework.network.impl.simpletcp;

import com.offbynull.eventframework.network.impl.message.Response;
import com.offbynull.peernetic.eventframework.event.DefaultSuccessIncomingEvent;

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
