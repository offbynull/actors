package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.SetPredecessorRequest;
import com.offbynull.peernetic.chord.messages.SetPredecessorResponse;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.util.MessageUtils;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;

public final class NotifyProcessor extends
        ProcessorAdapter<SetPredecessorResponse, Boolean> {

    private Pointer base;

    public NotifyProcessor(Pointer base, Pointer dest) {
        if (base == null || dest == null) {
            throw new NullPointerException();
        }
        
        this.base = base;
        
        Processor proc = new SendMessageProcessor(dest.getAddress().getHost(),
                dest.getAddress().getPort(), new SetPredecessorRequest(),
                SetPredecessorResponse.class);
        setProcessor(proc);
    }
    
    @Override
    protected Boolean onResult(SetPredecessorResponse res) throws Exception {
        NodePointer otherPredNp = res.getAssignedPredecessor();        
        Pointer otherPred = MessageUtils.convertTo(otherPredNp, false);
        return base.equals(otherPred);
    }

    @Override
    protected Boolean onException(Exception e) throws Exception {
        throw new NotifyFailedException();
    }

    public static class NotifyFailedException extends ProcessorException {
        
    }
}
