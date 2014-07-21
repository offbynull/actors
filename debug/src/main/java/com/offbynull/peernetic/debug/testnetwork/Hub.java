package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.debug.testnetwork.messages.TransitMessage;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.debug.testnetwork.messages.ArriveMessage;
import com.offbynull.peernetic.debug.testnetwork.messages.JoinHub;
import com.offbynull.peernetic.debug.testnetwork.messages.LeaveHub;
import com.offbynull.peernetic.debug.testnetwork.messages.StartHub;
import com.offbynull.peernetic.debug.testnetwork.messages.DepartMessage;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Instant;
import java.util.Collection;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public final class Hub<A> {

    public static final String INITIAL_STATE = "INITIAL";
    public static final String RUN_STATE = "RUN";

    private BidiMap<A, Endpoint> joinedNodes;
    private Line<A> line;
    private EndpointScheduler endpointScheduler;
    private Endpoint selfEndpoint;

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, StartHub message, Endpoint srcEndpoint) {
        joinedNodes = new DualHashBidiMap<>();
        line = message.getLineFactory().createLine();
        endpointScheduler = message.getEndpointScheduler();
        selfEndpoint = message.getSelfEndpoint();

        fsm.setState(RUN_STATE);
    }

    @StateHandler(RUN_STATE)
    public void handleJoin(String state, FiniteStateMachine fsm, Instant instant, JoinHub<A> message, Endpoint srcEndpoint) {
        A address = message.getAddress();

        joinedNodes.put(address, srcEndpoint);
    }

    @StateHandler(RUN_STATE)
    public void handleLeave(String state, FiniteStateMachine fsm, Instant instant, LeaveHub<A> message, Endpoint srcEndpoint) {
        A address = message.getAddress();

        joinedNodes.remove(address, srcEndpoint);
    }

    @StateHandler(RUN_STATE)
    public void handleDepart(String state, FiniteStateMachine fsm, Instant instant, DepartMessage<A> message, Endpoint srcEndpoint) {
        Collection<TransitMessage<A>> msgs = line.depart(message);
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
    public void handleTransit(String state, FiniteStateMachine fsm, Instant instant, TransitMessage<A> message, Endpoint srcEndpoint) {
        if (srcEndpoint != selfEndpoint) { // Sanity check
            return;
        }
        
        Collection<ArriveMessage<A>> msgs = line.arrive(message);
        msgs.forEach(x -> {
            A destinationId = x.getDestination();
            Endpoint destinationEndpoint = joinedNodes.get(destinationId);
            if (destinationEndpoint == null) {
                return;
            }
            
            A sourceId = x.getSource();
            
            Endpoint endpointToSendTo = new HubToNodeEndpoint<>(destinationEndpoint);
            Endpoint endpointForResponses = new NodeToHubEndpoint(selfEndpoint, destinationId, sourceId);
            
            endpointToSendTo.send(endpointForResponses, x.getData());
        });
    }
}
