package com.offbynull.peernetic.debug.actornetwork;

import com.offbynull.peernetic.debug.actornetwork.messages.TransitMessage;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.debug.actornetwork.Line.BufferMessage;
import com.offbynull.peernetic.debug.actornetwork.messages.JoinHub;
import com.offbynull.peernetic.debug.actornetwork.messages.LeaveHub;
import com.offbynull.peernetic.debug.actornetwork.messages.StartHub;
import com.offbynull.peernetic.debug.actornetwork.messages.DepartMessage;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import com.offbynull.peernetic.network.Serializer;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public final class Hub<A> {

    public static final String INITIAL_STATE = "INITIAL";
    public static final String RUN_STATE = "RUN";

    private BidiMap<A, Endpoint> joinedNodes;
    private Line<A> line;
    private Serializer serializer;
    private EndpointScheduler endpointScheduler;
    private Endpoint selfEndpoint;

    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, StartHub message, Endpoint srcEndpoint) {
        joinedNodes = new DualHashBidiMap<>();
        line = message.getLine();
        serializer = message.getSerializer();
        endpointScheduler = message.getEndpointScheduler();
        selfEndpoint = message.getSelfEndpoint();

        fsm.setState(RUN_STATE);
    }

    @StateHandler(RUN_STATE)
    public void handleJoin(FiniteStateMachine fsm, Instant time, JoinHub<A> message, Endpoint srcEndpoint) {
        A address = message.getAddress();

        line.nodeJoin(address);
        
        joinedNodes.put(address, srcEndpoint);
    }

    @StateHandler(RUN_STATE)
    public void handleLeave(FiniteStateMachine fsm, Instant time, LeaveHub<A> message, Endpoint srcEndpoint) {
        A address = message.getAddress();

        line.nodeLeave(address);
        
        joinedNodes.remove(address, srcEndpoint);
    }

    @StateHandler(RUN_STATE)
    public void handleDepart(FiniteStateMachine fsm, Instant time, DepartMessage<A> message, Endpoint srcEndpoint) {
        byte[] data = serializer.serialize(message.getData());
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        BufferMessage<A> bufferMessage = new BufferMessage<>(dataBuffer, message.getSource(), message.getDestination());
        
        Collection<TransitMessage<A>> msgs = line.messageDepart(time, bufferMessage);
        msgs.forEach(x -> {
            A source = message.getSource();
            Endpoint sourceEndpoint = joinedNodes.get(source);
            if (sourceEndpoint == null) {
                return;
            }
        
            endpointScheduler.scheduleMessage(x.getDuration(), selfEndpoint, selfEndpoint, x);
        });
    }

    @StateHandler(RUN_STATE)
    public void handleTransit(FiniteStateMachine fsm, Instant time, TransitMessage<A> message, Endpoint srcEndpoint) {
        if (srcEndpoint != selfEndpoint) { // Sanity check
            return;
        }
        
        Collection<BufferMessage<A>> msgs = line.messageArrive(time, message);
        msgs.forEach(x -> {
            A destinationId = x.getDestination();
            Endpoint destinationEndpoint = joinedNodes.get(destinationId);
            if (destinationEndpoint == null) {
                return;
            }
            
            A sourceId = x.getSource();
            
            byte[] data = ByteBufferUtils.copyContentsToArray(x.getData());
            Object dataObj = serializer.deserialize(data);
            
            Endpoint endpointForResponses = new NodeToHubEndpoint(selfEndpoint, destinationId, sourceId);
            destinationEndpoint.send(endpointForResponses, dataObj);
        });
    }
}
