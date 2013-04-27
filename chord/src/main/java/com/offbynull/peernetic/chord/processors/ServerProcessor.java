package com.offbynull.peernetic.chord.processors;

import com.offbynull.peernetic.p2ptools.overlay.chord.ChordState;
import com.offbynull.peernetic.chord.messages.SetPredecessorRequest;
import com.offbynull.peernetic.chord.messages.SetPredecessorResponse;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.chord.messages.util.MessageUtils;
import com.offbynull.peernetic.eventframework.impl.network.message.Request;
import com.offbynull.peernetic.eventframework.impl.network.message.Response;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveMessageIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveMessageProcessor;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveMessageProcessor.ReceiveMessageException;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveMessageProcessor.ReceiveMessageResponseFactory;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendResponseOutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.ProcessorAdapter;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import java.util.HashSet;
import java.util.Set;

public final class ServerProcessor extends ProcessorAdapter<Object, Object> {
    
    private ChordState chordState;

    public ServerProcessor(ChordState chordState, int port) {
        if (chordState == null) {
            throw new NullPointerException();
        }
        
        this.chordState = chordState;
        
        ReceiveMessageResponseFactory responseFactory =
                new CustomReceiveMessageResponseFactory();
        ReceiveMessageProcessor backingProc = new ReceiveMessageProcessor(
                responseFactory, port);
        setProcessor(backingProc);
    }

    @Override
    protected Object onResult(Object res) throws Exception {
        return null;
    }

    @Override
    protected Object onException(Exception e) throws Exception {
        if (e instanceof ReceiveMessageException) {
            throw new ServerException();
        }
        
        throw e;
    }
    
    private Response generateResponse(StatusRequest req) {
        StatusResponse srResp = new StatusResponse();

        BitLimitedId id = chordState.getBaseId();
        NodeId nid = MessageUtils.createFrom(id);
        srResp.setId(nid);

        BitLimitedPointer pred = chordState.getPredecessor();
        NodePointer nPred = MessageUtils.createFrom(pred);
        srResp.setPredecessor(nPred);

        Set<NodePointer> nFingers = new HashSet<>();
        int bitCnt = chordState.getBitCount();
        for (int i = 0; i < bitCnt; i++) {
            BitLimitedPointer finger = chordState.getFinger(i);
            NodePointer nFinger = MessageUtils.createFrom(finger);
            nFingers.add(nFinger);
        }
        srResp.setPointers(nFingers);

        return srResp;
    }

    private Response generateResponse(SetPredecessorRequest spReq) {
        SetPredecessorResponse spResp = new SetPredecessorResponse();

        NodePointer nNewPred = spReq.getPredecessor();
        BitLimitedPointer newPred = MessageUtils.convertTo(nNewPred);
        try {
            chordState.setPredecessor(newPred);
        } catch (Exception e) {
            // ignore exception
        }

        BitLimitedPointer pred = chordState.getPredecessor();
        NodePointer nPred = MessageUtils.createFrom(pred);
        spResp.setAssignedPredecessor(nPred);

        return spResp;
    }
        
    public final class ServerException extends ProcessorException {
        
    }
    
    private final class CustomReceiveMessageResponseFactory
            implements ReceiveMessageResponseFactory {

        @Override
        public SendResponseOutgoingEvent createResponse(
                ReceiveMessageIncomingEvent event) {
            
            Request req = event.getRequest();
            Response resp = null;
            
            if (req instanceof StatusRequest) {
                resp = generateResponse((StatusRequest) req);
            } else if (req instanceof SetPredecessorRequest) {
                resp = generateResponse((SetPredecessorRequest) req);
            }
            
            
            long pendingId = event.getPendingId();
            long trackedId = event.getTrackedId();
            
            return resp == null ? null :
                    new SendResponseOutgoingEvent(resp, pendingId, trackedId);
        }
    } 
}
