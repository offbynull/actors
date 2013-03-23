package com.offbynull.peernetic.eventframework.impl.network.address;

import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import java.util.Set;

public final class AddressResolveSimplifierProcessor implements Processor {

    private AddressResolveProcessor backingProc;

    public AddressResolveSimplifierProcessor(String host) {
        backingProc = new AddressResolveProcessor(host);
    }
    
    @Override
    public ProcessResult process(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        ProcessResult pr = backingProc.process(timestamp, event, trackedIdGen);
        
        if (pr instanceof FinishedProcessResult) {
            @SuppressWarnings("unchecked")
            FinishedProcessResult<Set<ResolvedAddress>> fpr =
                    (FinishedProcessResult<Set<ResolvedAddress>>) pr;
            
            Set<ResolvedAddress> resAddrs = fpr.getResult();
            AddressResolvedIncomingEvent arie =
                    new AddressResolvedIncomingEvent(resAddrs);
            
            return new FinishedProcessResult<>(arie);
        }
        
        return pr;
    }
}
