package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor.SendMessageException;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorAdapter;

public final class PingProcessor
        extends ProcessorAdapter<StatusResponse, Boolean> {
    
    public PingProcessor(Address address) {
        if (address == null) {
            throw new NullPointerException();
        }
        
        @SuppressWarnings("unchecked")
        Processor proc = new SendMessageProcessor(address.getHost(),
                address.getPort(), new StatusRequest(), StatusResponse.class);

        setProcessor(proc);
    }

    @Override
    protected Boolean onResult(StatusResponse res) throws Exception {
        return true;
    }

    @Override
    protected Boolean onException(Exception e) throws Exception {
        if (e instanceof SendMessageException) {
            return false;
        }
        
        throw e;
    }
}
