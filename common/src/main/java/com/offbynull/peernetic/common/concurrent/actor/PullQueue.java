package com.offbynull.peernetic.common.concurrent.actor;

import com.offbynull.peernetic.common.concurrent.actor.helpers.TimeoutManager;
import java.util.ArrayList;
import org.apache.commons.lang3.Validate;

public final class PullQueue {
    private TimeoutManager<Object> responseTimeoutManager;
    private ArrayList<Incoming> incomingList;
    private int requestPointer;
    private int responsePointer;

    PullQueue(TimeoutManager<Object> responseTimeoutManager, ArrayList<Incoming> incomingList) {
        Validate.notNull(responseTimeoutManager);
        Validate.noNullElements(incomingList);
        
        this.responseTimeoutManager = responseTimeoutManager;
        this.incomingList = incomingList;
    }

    public IncomingRequest pullRequest() {
        while (requestPointer < incomingList.size()) {
            Incoming incoming = incomingList.get(requestPointer);
            requestPointer++;
            
            if (incoming instanceof IncomingRequest) {
                return (IncomingRequest) incoming;
            }
        }
        
        return null;
    }
    
    public IncomingResponse pullResponse() {
        while (responsePointer < incomingList.size()) {
            Incoming incoming = incomingList.get(responsePointer);
            responsePointer++;
            
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
