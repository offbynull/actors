package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.demo.messages.external.FailureResponse;
import com.offbynull.peernetic.demo.messages.external.JoinNodeRequest;
import com.offbynull.peernetic.demo.messages.external.LinkRequest;
import com.offbynull.peernetic.demo.messages.external.KeepAliveRequest;
import com.offbynull.peernetic.demo.messages.external.LeaveRequest;
import com.offbynull.peernetic.demo.messages.external.SuccessResponse;
import com.offbynull.peernetic.demo.messages.internal.JoinNextAddress;
import com.offbynull.peernetic.demo.messages.internal.StartJoin;
import com.offbynull.peernetic.demo.messages.internal.StartSeed;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> {

    private static final String INITIAL_STATE = "INITIAL";
    private static final String ACTIVE_STATE = "ACTIVE";
    
    private static final int MAX_INCOMING_JOINS = 32;
    private static final int MAX_OUTGOING_JOINS = 32;
    
    
    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private LinkedHashSet<A> addressCache;
    private Map<String, A> incomingJoinedNodes;
    private Map<String, A> outgoingJoinedNodes;
    private Endpoint selfEndpoint;
    private Random random;

    public UnstructuredClient(EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier,
            EndpointScheduler endpointScheduler) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.addressCache = new LinkedHashSet<>();
        
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae);
        }
    }

    @StateHandler(INITIAL_STATE)
    public void handleInitialSeed(String state, FiniteStateMachine fsm, Instant instant, StartSeed<A> message, Object param) {
        
    }

    @StateHandler(INITIAL_STATE)
    public void handleInitialJoin(String state, FiniteStateMachine fsm, Instant instant, StartJoin message, Object param) {
        selfEndpoint = message.getSelfEndpoint();
        addressCache = new LinkedHashSet<>(message.getBootstrapAddresses());
        outgoingJoinedNodes = new HashMap<>();
        incomingJoinedNodes = new HashMap<>();
        
        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new JoinNextAddress(), null);
    }

    @StateHandler(ACTIVE_STATE)
    public void handleSearch(String state, FiniteStateMachine fsm, Instant instant, JoinNextAddress message, Object param) {
        // Get next address from address cache
        Iterator<A> addressCacheIt = addressCache.iterator();
        A address = addressCacheIt.next();
        addressCacheIt.remove();
        
        // Send join msg to address
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        Object dstMessage = new JoinNodeRequest(RandomStringUtils.random(32, 0, 32, true, true, null, random));
        dstEndpoint.send(selfEndpoint, dstMessage);
        
        // Reschedule this state to be triggered again in 5 seconds
        endpointScheduler.scheduleMessage(Duration.ofSeconds(5L), selfEndpoint, selfEndpoint, new JoinNextAddress());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleJoinRequests(String state, FiniteStateMachine fsm, Instant instant, LinkRequest message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint);
        
        if (incomingJoinedNodes.size() >= MAX_INCOMING_JOINS) {
            return;
        }
        
        String key = message.getKey();
        incomingJoinedNodes.put(key, address);
        
        srcEndpoint.send(selfEndpoint, new SuccessResponse(message));
    }

    @StateHandler(ACTIVE_STATE)
    public void handleKeepAliveRequests(String state, FiniteStateMachine fsm, Instant instant, KeepAliveRequest message, Endpoint srcEndpoint) {
        // TODO: use endpointscheduler to schedule some cleanup instruction to happen every x seconds. If this message is sent for a joined client, that client will not be cleaned up when the schedule runs
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLeaveRequests(String state, FiniteStateMachine fsm, Instant instant, LeaveRequest message, Endpoint srcEndpoint) {
        String key = message.getKey();
        incomingJoinedNodes.remove(key);
        
        srcEndpoint.send(selfEndpoint, new SuccessResponse(message));
    }

    @StateHandler(ACTIVE_STATE)
    public void handleSuccessResponse(String state, FiniteStateMachine fsm, Instant instant, SuccessResponse message, Endpoint srcEndpoint) {
    }

    @StateHandler(ACTIVE_STATE)
    public void handleFailureResponse(String state, FiniteStateMachine fsm, Instant instant, FailureResponse message, Endpoint srcEndpoint) {
    }
}
