package com.offbynull.eventframework.network.simpletcp;

import com.offbynull.peernetic.eventframework.event.DefaultTrackedOutgoingEvent;

public final class StopServerOutgoingEvent extends DefaultTrackedOutgoingEvent {

    public StopServerOutgoingEvent(long trackedId) {
        super(trackedId);
    }
}
