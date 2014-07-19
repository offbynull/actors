package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.NonceManager;
import com.offbynull.peernetic.SessionManager;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.demo.messages.external.LinkRequest;
import com.offbynull.peernetic.demo.messages.external.LinkResponse;
import com.offbynull.peernetic.demo.messages.external.Request;
import com.offbynull.peernetic.demo.messages.internal.StartJoin;
import com.offbynull.peernetic.demo.messages.internal.StartSeed;
import com.offbynull.peernetic.demo.messages.internal.Timer;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.PreStateHandler;
import com.offbynull.peernetic.fsm.StateHandler;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> {

    private static final String INITIAL_STATE = "INITIAL";
    private static final String ACTIVE_STATE = "ACTIVE";
    
    private static final Duration SESSION_DURATION = Duration.ofSeconds(30L);
    
    private static final int MAX_INCOMING_JOINS = 32;
    private static final int MAX_OUTGOING_JOINS = 32;
    
    
    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    
    private LinkedHashSet<A> addressCache;
    private NonceManager<A> nonceManager;
    private SessionManager<A> incomingSessions;
    private SessionManager<A> outgoingSessions;
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
        incomingSessions = new SessionManager<>();
        outgoingSessions = new SessionManager<>();
        nonceManager = new NonceManager<>();
        
        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new Timer(), null);
    }

    @PreStateHandler(ACTIVE_STATE)
    public boolean checkIfHandled(String state, FiniteStateMachine fsm, Instant instant, Request message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint);
        Optional<Object> pastResponse = nonceManager.checkNonce(instant, address);
        
        if (pastResponse.isPresent()) {
            Object response = pastResponse.get();
            srcEndpoint.send(selfEndpoint, response);
            return false;
        }
        
        return true;
    }
    
    @StateHandler(ACTIVE_STATE)
    public void handleSearch(String state, FiniteStateMachine fsm, Instant instant, Timer message, Object param) {
        // TODO:
        //
        // Check to see if you have open outgoing slots, if so, send requests to addresses in cache
        // Prune session managers, nonce managers
        // Reschedule endpoint
        
        // Get next address from address cache
        Iterator<A> addressCacheIt = addressCache.iterator();
        A address = addressCacheIt.next();
        addressCacheIt.remove();
        
        // Send join msg to address
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        Object dstMessage = new JoinNodeRequest(RandomStringUtils.random(32, 0, 32, true, true, null, random));
        dstEndpoint.send(selfEndpoint, dstMessage);
        
        // Reschedule this state to be triggered again in 5 seconds
        endpointScheduler.scheduleMessage(Duration.ofSeconds(5L), selfEndpoint, selfEndpoint, new Timer());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkRequests(String state, FiniteStateMachine fsm, Instant instant, LinkRequest message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint);
        List<A> links = incomingSessions.getSessions();
        
        if (incomingSessions.size() >= MAX_INCOMING_JOINS) {
            srcEndpoint.send(selfEndpoint, new LinkResponse<>(false, links, message.getNonce()));
            return;
        }
        
        incomingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
        
        srcEndpoint.send(selfEndpoint, new LinkResponse(true, links, message.getNonce()));
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkResponse(String state, FiniteStateMachine fsm, Instant instant, LinkResponse message, Endpoint srcEndpoint) {
        message.validate();
        
        if (outgoingSessions.size() >= MAX_OUTGOING_JOINS) {
            return; // ignore
        }
        
        A address = endpointIdentifier.identify(srcEndpoint);
        if (message.isSuccessful()) {
            outgoingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
        }
        
        addressCache.addAll(message.getLinks());
    }
}
