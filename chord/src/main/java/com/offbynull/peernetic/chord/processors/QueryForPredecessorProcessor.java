package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.util.MessageUtils;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor.SendMessageException;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.eventframework.processor.ProcessorUtils;
import java.util.Set;

public final class QueryForPredecessorProcessor implements Processor {
    
    private Address address;
    private SendMessageProcessor backingProc;

    public QueryForPredecessorProcessor(Address address) {
        if (address == null) {
            throw new NullPointerException();
        }
        
        backingProc = new SendMessageProcessor(address.getHost(),
                address.getPort(), new StatusRequest(), StatusResponse.class);
        this.address = address;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) {
        ProcessResult pr;
        
        try {
            pr = backingProc.process(timestamp, event, trackedIdGen);
        } catch (SendMessageException e) {
            throw new QueryForPredecessorException();
        }
        
        StatusResponse resp = ProcessorUtils.extractFinishedResult(pr);
        if (resp != null) {
            // got response
            NodePointer predNp = resp.getPredecessor();

            // reconstruct finger table
            Pointer ret = MessageUtils.convertTo(predNp, false);

            return new FinishedProcessResult<>(ret);
        }
        
        return pr;
    }
    
    public static class QueryForPredecessorException
            extends ProcessorException {
        
    }
}
