package com.offbynull.peernetic.eventframework.handler.communication;

import com.offbynull.peernetic.eventframework.event.DefaultTrackedOutgoingEvent;

public final class StopServerOutgoingEvent extends DefaultTrackedOutgoingEvent {

    public StopServerOutgoingEvent(long trackedId) {
        super(trackedId);
    }
}
