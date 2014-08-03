package com.offbynull.peernetic.demos.unstructured;

import com.offbynull.peernetic.common.AddressCache;
import com.offbynull.peernetic.common.AddressCache.RetentionMode;
import com.offbynull.peernetic.common.ByteArrayNonce;
import com.offbynull.peernetic.common.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.Nonce;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceManager;
import com.offbynull.peernetic.common.SessionManager;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.Message;
import com.offbynull.peernetic.common.Request;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.unstructured.messages.external.LinkRequest;
import com.offbynull.peernetic.demos.unstructured.messages.external.LinkResponse;
import com.offbynull.peernetic.demos.unstructured.messages.external.QueryRequest;
import com.offbynull.peernetic.demos.unstructured.messages.external.QueryResponse;
import com.offbynull.peernetic.demos.unstructured.messages.internal.Start;
import com.offbynull.peernetic.demos.unstructured.messages.internal.Timer;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

    private static final int MAX_INCOMING_JOINS = 3;
    private static final int MAX_OUTGOING_JOINS = 3;

    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private AddressCache<A> addressCache;
    private NonceGenerator<byte[]> nonceGenerator;
    private NonceManager<byte[]> incomingRequestsNonceManager;
    private NonceManager<byte[]> outgoingLinkRequestsNonceManager;
    private NonceManager<byte[]> outgoingQueryRequestsNonceManager;
    private SessionManager<A> incomingSessions;
    private SessionManager<A> outgoingSessions;
    private Endpoint selfEndpoint;
    private A selfAddress;

    private UnstructuredClientListener<A> listener;

    public UnstructuredClient(UnstructuredClientListener<A> listener) {
        Validate.notNull(listener);
        this.listener = listener;
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Start<A> message, Endpoint srcEndpoint) {
        endpointDirectory = message.getEndpointDirectory();
        endpointIdentifier = message.getEndpointIdentifier();
        endpointScheduler = message.getEndpointScheduler();
        selfEndpoint = message.getSelfEndpoint();
        selfAddress = message.getSelfAddress();
        addressCache = new AddressCache<>(1, 256, message.getBootstrapAddresses(), RetentionMode.RETAIN_OLDEST);
        incomingSessions = new SessionManager<>();
        outgoingSessions = new SessionManager<>();
        nonceGenerator = new ByteArrayNonceGenerator(Message.NONCE_LENGTH);
        incomingRequestsNonceManager = new NonceManager<>();
        outgoingLinkRequestsNonceManager = new NonceManager<>();
        outgoingQueryRequestsNonceManager = new NonceManager<>();

        listener.onStarted(selfAddress);

        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new Timer(), null);
    }

    @FilterHandler(ACTIVE_STATE)
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

    @FilterHandler(ACTIVE_STATE)
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

        prunedOutgoingLinks.forEach(prunedAddress -> listener.onDisconnected(selfAddress, prunedAddress));

        // If address cache is at minimum capacity, ask a neighbour for more nodes (if neighbours available)
        if (addressCache.isMinimumCapacity()) {
            List<A> links = getAllSessions();
            if (!links.isEmpty()) {
                // Grab random link
                int pos = RandomUtils.nextInt(0, links.size());
                A address = links.get(pos);

                sendQueryRequest(instant, address);
            }
        }

        // Send a message to all outgoing links telling them we're still alive, otherwise we'll get dropped
        outgoingSessions.getSessions().forEach(address -> sendLinkRequest(instant, address));

        // Check to see if we have room for more outgoing links. If we do, send out new link requests
        int openOutgoingSlots = MAX_OUTGOING_JOINS - outgoingSessions.size();
        int requestCount = Math.min(openOutgoingSlots, addressCache.size());
        
        for (int i = 0; i < requestCount; i++) {
            // Get next address from address cache
            A address = addressCache.next();

            // If already have a link to address, skip
            if (outgoingSessions.containsSession(address) || incomingSessions.containsSession(address)) {
                continue;
            }

            sendLinkRequest(instant, address);
        }

        // Reschedule this state to be triggered again in 5 seconds
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new Timer());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkRequest(String state, FiniteStateMachine fsm, Instant instant, LinkRequest message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint); // we can also use srcEndpoint to send responses back directly
        
        if (!incomingSessions.containsSession(address) && incomingSessions.size() >= MAX_INCOMING_JOINS) {
            // If the address isn't already an incoming link and no space available for new incoming links, send rejection
            sendLinkResponse(instant, address, message.getNonce(), false);
        } else {
            // Otherwise, add or update session and send successful response
            incomingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
            sendLinkResponse(instant, address, message.getNonce(), true);
        }

    }

    @StateHandler(ACTIVE_STATE)
    public void handleQueryRequest(String state, FiniteStateMachine fsm, Instant instant, QueryRequest message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint); // we can also use srcEndpoint to send responses back directly
        
        sendQueryResponse(instant, address, message.getNonce());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkResponse(String state, FiniteStateMachine fsm, Instant instant, LinkResponse message, Endpoint srcEndpoint) {
        // The filter should have checked to make sure that this is a response to a rquest we sent out
        A dstAddress = endpointIdentifier.identify(srcEndpoint);

        boolean alreadyExists = outgoingSessions.containsSession(dstAddress);
        if (message.isSuccessful()) {
            if (!alreadyExists) { // If new connection
                if (outgoingSessions.size() < MAX_OUTGOING_JOINS) { // If space is available for new connection
                    outgoingSessions.addOrUpdateSession(instant, SESSION_DURATION, dstAddress, null);
                    listener.onOutgoingConnected(selfAddress, dstAddress);
                }
            } else { // If existing connection
                outgoingSessions.addOrUpdateSession(instant, SESSION_DURATION, dstAddress, null);
            }
        }

        addressCache.addAll(message.getLinks());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleQueryResponse(String state, FiniteStateMachine fsm, Instant instant, QueryResponse message, Endpoint srcEndpoint) {
        // The filter should have checked to make sure that this is a response to a rquest we sent out
        addressCache.addAll(message.getLinks());
    }

    private List<A> getAllSessions() {
        List<A> inLinks = incomingSessions.getSessions();
        List<A> outLinks = outgoingSessions.getSessions();

        List<A> links = new ArrayList<>();
        links.addAll(inLinks);
        links.addAll(outLinks);

        return links;
    }
    
    private void sendLinkRequest(Instant instant, A address) {
        // Send link message to address
        Nonce<byte[]> nonce = nonceGenerator.generate();
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        Object dstMessage = new LinkRequest(nonce.getValue());
        dstEndpoint.send(selfEndpoint, dstMessage);

        // Ensure we ignore the request if we're sending to ourselves
        outgoingLinkRequestsNonceManager.addNonce(instant, NONCE_DURATION, nonce, null);
    }

    private void sendQueryRequest(Instant instant, A address) {
        // Send query message to address
        Nonce<byte[]> nonce = nonceGenerator.generate();
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        Object dstMessage = new QueryRequest(nonce.getValue());
        dstEndpoint.send(selfEndpoint, dstMessage);

        // Ensure we ignore the request if we're sending to ourselves
        outgoingQueryRequestsNonceManager.addNonce(instant, NONCE_DURATION, nonce, null);
    }
    
    private void sendLinkResponse(Instant instant, A address, byte[] nonce, boolean successful) {
        List<A> links = getAllSessions();
        
        // Send response to requester
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        LinkResponse<A> response = new LinkResponse(successful, links, nonce);
        dstEndpoint.send(selfEndpoint, response);
        
        // Ensure duplicate requests get same response
        incomingRequestsNonceManager.addNonce(instant, NONCE_DURATION, new ByteArrayNonce(nonce), response);
    }

    private void sendQueryResponse(Instant instant, A address, byte[] nonce) {
        List<A> links = getAllSessions();
        
        // Send response to requester
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        QueryResponse<A> response = new QueryResponse(links, nonce);
        dstEndpoint.send(selfEndpoint, response);
        
        // Ensure duplicate requests get same response
        incomingRequestsNonceManager.addNonce(instant, NONCE_DURATION, new ByteArrayNonce(nonce), response);
    }
}
