package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.util.MessageUtils;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageProcessor.SendMessageException;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.p2ptools.identification.Address;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.structured.chord.FingerTable;
import java.util.Set;

public final class QueryForFingerTableProcessor
        extends ProcessorAdapter<StatusResponse, FingerTable> {

    private Address address;

    public QueryForFingerTableProcessor(Address address) {
        if (address == null) {
            throw new NullPointerException();
        }


        this.address = address;
        Processor proc = new SendMessageProcessor(address.getIpAsString(),
                address.getPort(), new StatusRequest(), StatusResponse.class);

        setProcessor(proc);
    }

    @Override
    protected FingerTable onResult(StatusResponse res) {
        // got response
        NodeId nodeId = res.getId();
        Set<NodePointer> nodePtrs = res.getPointers();

        // reconstruct finger table
        FingerTable fingerTable;
        try {
            BitLimitedId id = MessageUtils.convertTo(nodeId, false);
            BitLimitedPointer ptr = new BitLimitedPointer(id, address);
            fingerTable = new FingerTable(ptr);
            for (NodePointer pointer : nodePtrs) {
                BitLimitedPointer fingerPtr = MessageUtils.convertTo(pointer, false);

                fingerTable.put(fingerPtr);
            }
        } catch (RuntimeException re) {
            throw new QueryForFingerTableException();
        }
        
        return fingerTable;
    }

    @Override
    protected FingerTable onException(Exception e) throws Exception {
        if (e instanceof SendMessageException) {
            throw new QueryForFingerTableException();
        }

        throw e;
    }

    public static class QueryForFingerTableException
            extends ProcessorException {
    }
}
