package com.offbynull.peernetic.eventframework.impl.network.simpletcp;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.message.Response;

public final class NetEventUtils {
    
    @SuppressWarnings("unchecked")
    public static <T extends Response> T testAndConvertResponse(
            IncomingEvent inEvent, long trackedId) {
        
        if (inEvent instanceof TrackedIncomingEvent) {
            TrackedIncomingEvent tie = (TrackedIncomingEvent) inEvent;

            if (trackedId == tie.getTrackedId()
                    && ReceiveResponseIncomingEvent.class.isInstance(inEvent)) {
                return (T) ((ReceiveResponseIncomingEvent) tie).getResponse();
            }
        }
        
        return null;
    }
}
