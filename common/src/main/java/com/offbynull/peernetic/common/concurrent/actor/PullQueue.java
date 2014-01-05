package com.offbynull.peernetic.common.concurrent.actor;

import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.lang3.Validate;

public final class PullQueue {
    private TimeoutManager<Object> responseTimeoutManager;
    private Iterator<Incoming> requestPointer;
    private Iterator<Incoming> responsePointer;

    PullQueue(TimeoutManager<Object> responseTimeoutManager, Collection<Incoming> incoming) {
        Validate.notNull(responseTimeoutManager);
        Validate.noNullElements(incoming);
        
        this.responseTimeoutManager = responseTimeoutManager;
        this.requestPointer = incoming.iterator();
        this.responsePointer = incoming.iterator();
    }

    public IncomingRequest pullRequest() {
        while (requestPointer.hasNext()) {
            Incoming incoming = requestPointer.next();
            
            if (incoming instanceof IncomingRequest) {
                return (IncomingRequest) incoming;
            }
        }
        
        return null;
    }
    
    public IncomingResponse pullResponse() {
        while (responsePointer.hasNext()) {
            Incoming incoming = responsePointer.next();
            
            if (incoming instanceof IncomingResponse) {
                IncomingResponse response = (IncomingResponse) incoming;
                
                Object id = response.getId();
                boolean canceled = responseTimeoutManager.cancel(id);
                
                if (canceled) {
                    return (IncomingResponse) response;
                }
            }
        }
        
        return null;
    }
}
