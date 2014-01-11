package com.offbynull.peernetic.overlay.chord.tasks;

import com.offbynull.peernetic.actor.Incoming;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.RequestManager;
import com.offbynull.peernetic.actor.helpers.RequestManager.IncomingRequestHandler;
import com.offbynull.peernetic.actor.helpers.Task;
import com.offbynull.peernetic.overlay.chord.core.ChordState;
import com.offbynull.peernetic.overlay.chord.core.RouteResult;
import com.offbynull.peernetic.overlay.chord.messages.GetClosestPrecedingFinger;
import com.offbynull.peernetic.overlay.chord.messages.GetClosestPrecedingFingerReply;
import com.offbynull.peernetic.overlay.chord.messages.GetPredecessor;
import com.offbynull.peernetic.overlay.chord.messages.GetPredecessorReply;
import com.offbynull.peernetic.overlay.chord.messages.GetSuccessor;
import com.offbynull.peernetic.overlay.chord.messages.GetSuccessorReply;
import com.offbynull.peernetic.overlay.chord.messages.Notify;
import com.offbynull.peernetic.overlay.chord.messages.NotifyReply;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.Random;
import org.apache.commons.lang3.Validate;

public final class RespondTask<A> implements Task {

    private ChordState<A> chordState;
    
    private RequestManager requestManager;
    private TaskState state = TaskState.START;

    public RespondTask(Random random, ChordState<A> chordState) {
        Validate.notNull(chordState);
        Validate.notNull(random);
        this.chordState = chordState;
        this.requestManager = new RequestManager(random);
    }

    @Override
    public TaskState getState() {
        return state;
    }

    @Override
    public long process(long timestamp, Incoming incoming, PushQueue pushQueue) {
        switch (state) {
            case START: {
                requestManager.mapRequestHandler(GetClosestPrecedingFinger.class, new GetClosestPrecedingFingerRequestHandler());
                requestManager.mapRequestHandler(GetPredecessor.class, new GetPredecessorRequestHandler());
                requestManager.mapRequestHandler(GetSuccessor.class, new GetSuccessorRequestHandler());
                requestManager.mapRequestHandler(Notify.class, new NotifyRequestHandler());
                break;
            }
            case PROCESSING: {
                break;
            }
        }
        
        if (incoming != null) {
            requestManager.incomingMessage(timestamp, incoming);
        }
        
        return requestManager.process(timestamp, pushQueue);
    }
    
    private final class GetClosestPrecedingFingerRequestHandler implements IncomingRequestHandler<GetClosestPrecedingFinger> {

        @Override
        public Object produceResponse(long timestamp, GetClosestPrecedingFinger request) {
            RouteResult<A> result = chordState.route(request.getId());
            return new GetClosestPrecedingFingerReply<>(result.getPointer());
        }
    }
    private final class GetPredecessorRequestHandler implements IncomingRequestHandler<GetPredecessor> {

        @Override
        public Object produceResponse(long timestamp, GetPredecessor request) {
            return new GetPredecessorReply<>(chordState.getPredecessor());
        }
    }
    private final class GetSuccessorRequestHandler implements IncomingRequestHandler<GetSuccessor> {

        @Override
        public Object produceResponse(long timestamp, GetSuccessor request) {
            return new GetSuccessorReply<>(chordState.getSuccessor());
        }
    }
    private final class NotifyRequestHandler implements IncomingRequestHandler<Notify> {

        @Override
        public Object produceResponse(long timestamp, Notify request) {
            Pointer<A> newPred = request.getPredecessor();
            Pointer<A> currentPred = chordState.getPredecessor();
            
            Id selfId = chordState.getBaseId();
            Id newPredId = newPred.getId();
            Id currentPredId = currentPred.getId();
            if (newPredId.isWithin(currentPredId, false, selfId, false)) {
                chordState.setPredecessor(newPred);
            }
            
            return new NotifyReply();
        }
    }
}

