package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.ByteArrayNonce;
import com.offbynull.peernetic.ByteArrayNonceGenerator;
import com.offbynull.peernetic.Nonce;
import com.offbynull.peernetic.NonceGenerator;
import com.offbynull.peernetic.NonceManager;
import com.offbynull.peernetic.SessionManager;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.demo.messages.external.LinkRequest;
import com.offbynull.peernetic.demo.messages.external.LinkResponse;
import com.offbynull.peernetic.demo.messages.external.Message;
import com.offbynull.peernetic.demo.messages.external.QueryRequest;
import com.offbynull.peernetic.demo.messages.external.QueryResponse;
import com.offbynull.peernetic.demo.messages.external.Request;
import com.offbynull.peernetic.demo.messages.external.Response;
import com.offbynull.peernetic.demo.messages.internal.Start;
import com.offbynull.peernetic.demo.messages.internal.Timer;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.FilterStateHandler;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> {

    public static final String INITIAL_STATE = "INITIAL";
    public static final String ACTIVE_STATE = "ACTIVE";
    
    private static final Duration SESSION_DURATION = Duration.ofSeconds(30L);
    private static final Duration NONCE_DURATION = Duration.ofSeconds(10L);
    private static final Duration TIMER_DURATION = Duration.ofSeconds(2L);
    
    private static final int MAX_INCOMING_JOINS = 5;
    private static final int MAX_OUTGOING_JOINS = 5;
    
    
    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private LinkedHashSet<A> addressCache;
    private NonceGenerator<byte[]> nonceGenerator;
    private NonceManager<byte[]> incomingRequestsNonceManager;
    private NonceManager<byte[]> outgoingLinkRequestsNonceManager;
    private NonceManager<byte[]> outgoingQueryRequestsNonceManager;
    private SessionManager<A> incomingSessions;
    private SessionManager<A> outgoingSessions;
    private Endpoint selfEndpoint;
    
    private UnstructuredClientListener<A> listener;

    public UnstructuredClient(UnstructuredClientListener<A> listener) {
        Validate.notNull(listener);
        this.listener = listener;
    }
    

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Start message, Endpoint srcEndpoint) {
        endpointDirectory = message.getEndpointDirectory();
        endpointIdentifier = message.getEndpointIdentifier();
        endpointScheduler = message.getEndpointScheduler();
        selfEndpoint = message.getSelfEndpoint();
        addressCache = new LinkedHashSet<>(message.getBootstrapAddresses());
        incomingSessions = new SessionManager<>();
        outgoingSessions = new SessionManager<>();
        nonceGenerator = new ByteArrayNonceGenerator(Message.NONCE_LENGTH);
        incomingRequestsNonceManager = new NonceManager<>();
        outgoingLinkRequestsNonceManager = new NonceManager<>();
        outgoingQueryRequestsNonceManager = new NonceManager<>();
        
        A address = endpointIdentifier.identify(selfEndpoint);
        listener.onStarted(address);
        
        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new Timer(), null);
    }

    @FilterStateHandler(ACTIVE_STATE)
    public boolean checkIncomingRequest(String state, FiniteStateMachine fsm, Instant instant, Request message, Endpoint srcEndpoint) {
        // Check nonce to make sure it isn't a request from ourselves.
        Nonce<byte[]> nonce = new ByteArrayNonce(message.getNonce());
        if (outgoingLinkRequestsNonceManager.checkNonce(nonce) != null
                || outgoingQueryRequestsNonceManager.checkNonce(nonce) != null) {
            return false;
        }
        
        // Check nonce to make sure we haven't already responded to this message. If processed already, return cached response.
        Optional<Object> pastResponse = incomingRequestsNonceManager.checkNonce(nonce);
        if (pastResponse != null && pastResponse.isPresent()) {
            Object response = pastResponse.get();
            srcEndpoint.send(selfEndpoint, response);
            return false;
        }
        
        // Make sure message is valid. If not, don't process.
        try {
            message.validate();
        } catch (IllegalStateException ise) {
            return false;
        }
        
        return true;
    }

    @FilterStateHandler(ACTIVE_STATE)
    public boolean checkIncomingResponse(String state, FiniteStateMachine fsm, Instant instant, Response message, Endpoint srcEndpoint) {
        Nonce<byte[]> nonce = new ByteArrayNonce(message.getNonce());
        
        // Check nonce to make sure it this response is for a request we made
        NonceManager<byte[]> nonceManager;
        if (message instanceof LinkResponse) {
            nonceManager = outgoingLinkRequestsNonceManager;
        } else if (message instanceof QueryResponse) {
            nonceManager = outgoingQueryRequestsNonceManager;
        } else {
            return false;
        }
        
        if (nonceManager.checkNonce(nonce) == null) {
            return false;
        }
        nonceManager.removeNonce(nonce);

        // Make sure message is valid. If not, don't process.
        try {
            message.validate();
        } catch (IllegalStateException ise) {
            return false;
        }
        
        return true;
    }
    
    @StateHandler(ACTIVE_STATE)
    public void handleTimer(String state, FiniteStateMachine fsm, Instant instant, Timer message, Endpoint srcEndpoint) {
        // Prune nonce managers and session managers
        incomingRequestsNonceManager.prune(instant);
        outgoingLinkRequestsNonceManager.prune(instant);
        outgoingQueryRequestsNonceManager.prune(instant);
        incomingSessions.prune(instant);
        Set<A> prunedOutgoingLinks = outgoingSessions.prune(instant).keySet();
        
        A selfAddress = endpointIdentifier.identify(selfEndpoint);
        for (A otherAddress : prunedOutgoingLinks) {
            listener.onDisconnected(selfAddress, otherAddress);
        }
        
        
        
        // Check to see if you have open outgoing slots. If so, send requests to addresses in cache
        Iterator<A> addressCacheIt = addressCache.iterator();
        while (outgoingSessions.size() < MAX_OUTGOING_JOINS && addressCacheIt.hasNext()) {
            // Get next address from address cache
            A address = addressCacheIt.next();
            addressCacheIt.remove();
            
            // If already have a link to address, skip
            if (outgoingSessions.containsSession(address) || incomingSessions.containsSession(address)) {
                continue;
            }

            // Send link message to address
            Nonce<byte[]> nonce = nonceGenerator.generate();
            Endpoint dstEndpoint = endpointDirectory.lookup(address);
            Object dstMessage = new LinkRequest(nonce.getValue());
            dstEndpoint.send(selfEndpoint, dstMessage);
            
            // Track
            outgoingLinkRequestsNonceManager.addNonce(instant, NONCE_DURATION, nonce, null);
        }
        
        // If address cache is empty, ask a neighbour for more nodes
        List<A> links = generateAllLinks();
        if (addressCache.isEmpty() && !links.isEmpty()) {
            // Grab random link
            int pos = RandomUtils.nextInt(0, links.size());
            A address = links.get(pos);
            
            // Send query message to address
            Nonce<byte[]> nonce = nonceGenerator.generate();
            Endpoint dstEndpoint = endpointDirectory.lookup(address);
            Object dstMessage = new QueryRequest(nonce.getValue());
            dstEndpoint.send(selfEndpoint, dstMessage);
            
            // Track
            outgoingQueryRequestsNonceManager.addNonce(instant, NONCE_DURATION, nonce, null);
        }
        
        // Send a message to all outgoing links telling them we're still alive, otherwise we'll get dropped
        for (A address : outgoingSessions.getSessions()) {
            Nonce<byte[]> nonce = nonceGenerator.generate();
            Endpoint dstEndpoint = endpointDirectory.lookup(address);
            Object dstMessage = new LinkRequest(nonce.getValue());
            dstEndpoint.send(selfEndpoint, dstMessage);
            
            outgoingLinkRequestsNonceManager.addNonce(instant, NONCE_DURATION, nonce, null);
        }
        
        // Reschedule this state to be triggered again in 5 seconds
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new Timer());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkRequest(String state, FiniteStateMachine fsm, Instant instant, LinkRequest message, Endpoint srcEndpoint) {
        List<A> links = generateAllLinks();
        
        // If the address isn't already an incoming link and no space available for new incoming links, send rejection
        A address = endpointIdentifier.identify(srcEndpoint);
        if (!incomingSessions.containsSession(address) && incomingSessions.size() >= MAX_INCOMING_JOINS) {
            srcEndpoint.send(selfEndpoint, new LinkResponse<>(false, links, message.getNonce()));
            return;
        }
        
        // Otherwise, add or update session and send successful response
        incomingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
        
        LinkResponse<A> response = new LinkResponse(true, links, message.getNonce());
        srcEndpoint.send(selfEndpoint, response);
        incomingRequestsNonceManager.addNonce(instant, NONCE_DURATION, new ByteArrayNonce(message.getNonce()), response);
    }

    @StateHandler(ACTIVE_STATE)
    public void handleQueryRequest(String state, FiniteStateMachine fsm, Instant instant, QueryRequest message, Endpoint srcEndpoint) {
        List<A> links = generateAllLinks();
        
        QueryResponse<A> response = new QueryResponse(links, message.getNonce());
        srcEndpoint.send(selfEndpoint, response);
        incomingRequestsNonceManager.addNonce(instant, NONCE_DURATION, new ByteArrayNonce(message.getNonce()), response);
    }

    private List<A> generateAllLinks() {
        List<A> inLinks = incomingSessions.getSessions();
        List<A> outLinks = outgoingSessions.getSessions();
        
        List<A> links = new ArrayList<>();
        links.addAll(inLinks);
        links.addAll(outLinks);
        
        return links;
    }
    
    @StateHandler(ACTIVE_STATE)
    public void handleLinkResponse(String state, FiniteStateMachine fsm, Instant instant, LinkResponse message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint);
        boolean alreadyExists = outgoingSessions.containsSession(address);
        if (message.isSuccessful()) {
            if (!alreadyExists) { // If new connection
                if (outgoingSessions.size() < MAX_OUTGOING_JOINS) { // If space is available for new connection
                    outgoingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
                    listener.onOutgoingConnected(endpointIdentifier.identify(selfEndpoint), address);
                }
            } else { // If existing connection
                outgoingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
            }
        }
        
        addressCache.addAll(message.getLinks());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleQueryResponse(String state, FiniteStateMachine fsm, Instant instant, QueryResponse message, Endpoint srcEndpoint) {
        addressCache.addAll(message.getLinks());
    }
}
