package com.offbynull.peernetic.demo;

import com.offbynull.peernetic.ByteArrayNonce;
import com.offbynull.peernetic.ByteArrayNonceGenerator;
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
import com.offbynull.peernetic.demo.messages.external.Request;
import com.offbynull.peernetic.demo.messages.internal.Start;
import com.offbynull.peernetic.demo.messages.internal.Timer;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.PreStateHandler;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> {

    private static final String INITIAL_STATE = "INITIAL";
    private static final String ACTIVE_STATE = "ACTIVE";
    
    private static final Duration SESSION_DURATION = Duration.ofSeconds(30L);
    private static final Duration TIMER_DURATION = Duration.ofSeconds(5L);
    
    private static final int MAX_INCOMING_JOINS = 32;
    private static final int MAX_OUTGOING_JOINS = 32;
    
    
    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    
    private LinkedHashSet<A> addressCache;
    private NonceGenerator<byte[]> nonceGenerator;
    private NonceManager<byte[]> nonceManager;
    private SessionManager<A> incomingSessions;
    private SessionManager<A> outgoingSessions;
    private Endpoint selfEndpoint;

    public UnstructuredClient(EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier,
            EndpointScheduler endpointScheduler) {
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
    }

    @StateHandler(INITIAL_STATE)
    public void handleInitialJoin(String state, FiniteStateMachine fsm, Instant instant, Start message, Object param) {
        selfEndpoint = message.getSelfEndpoint();
        addressCache = new LinkedHashSet<>(message.getBootstrapAddresses());
        incomingSessions = new SessionManager<>();
        outgoingSessions = new SessionManager<>();
        nonceGenerator = new ByteArrayNonceGenerator(Message.NONCE_LENGTH);
        nonceManager = new NonceManager<>();
        
        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new Timer(), null);
    }

    @PreStateHandler(ACTIVE_STATE)
    public boolean checkIfHandled(String state, FiniteStateMachine fsm, Instant instant, Request message, Endpoint srcEndpoint) {
        // Check nonce to make sure we haven't already responded to this message. If processed already, return cached response.
        Optional<Object> pastResponse = nonceManager.checkNonce(instant, new ByteArrayNonce(message.getNonce()));
        
        if (pastResponse.isPresent()) {
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
    
    @StateHandler(ACTIVE_STATE)
    public void handleSearch(String state, FiniteStateMachine fsm, Instant instant, Timer message, Object param) {
        // Reschedule endpoint
        
        // Prune session managers, nonce managers
        nonceManager.prune(instant);
        outgoingSessions.prune(instant);
        incomingSessions.prune(instant);
        
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
            Endpoint dstEndpoint = endpointDirectory.lookup(address);
            Object dstMessage = new LinkRequest(nonceGenerator.generate().getValue());
            dstEndpoint.send(selfEndpoint, dstMessage);
            
            // Add to outgoing sessions, if response comes back as a failed join we can remove it then
            outgoingSessions.addSession(instant, SESSION_DURATION, address, param);
        }
        
        // Reschedule this state to be triggered again in 5 seconds
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new Timer());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkRequest(String state, FiniteStateMachine fsm, Instant instant, LinkRequest message, Endpoint srcEndpoint) {
        A address = endpointIdentifier.identify(srcEndpoint);
        List<A> links = incomingSessions.getSessions();
        
        // If no space available for incoming request
        if (incomingSessions.size() >= MAX_INCOMING_JOINS) {
            srcEndpoint.send(selfEndpoint, new LinkResponse<>(false, links, message.getNonce()));
            return;
        }
        
        incomingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
        
        srcEndpoint.send(selfEndpoint, new LinkResponse(true, links, message.getNonce()));
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkResponse(String state, FiniteStateMachine fsm, Instant instant, LinkResponse message, Endpoint srcEndpoint) {
        // If no space available anymore for outgoing request
        if (outgoingSessions.size() >= MAX_OUTGOING_JOINS) {
            return; // ignore
        }
        
        A address = endpointIdentifier.identify(srcEndpoint);
        
        // If the response is coming back from a address which we're not tracking, ignore it
        if (!outgoingSessions.containsSession(address)) {
            return;
        }
        
        if (message.isSuccessful()) {
            outgoingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
        } else {
            outgoingSessions.removeSession(instant, address);
        }
        
        addressCache.addAll(message.getLinks());
    }
}
