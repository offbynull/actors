package com.offbynull.peernetic.demos.unstructured;

import com.offbynull.peernetic.common.AddressCache;
import com.offbynull.peernetic.common.AddressCache.RetentionMode;
import com.offbynull.peernetic.common.SessionManager;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.ProcessableUtils;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.transmission.IncomingRequestManager;
import com.offbynull.peernetic.common.transmission.OutgoingRequestManager;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
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
import java.util.Set;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> {

    public static final String INITIAL_STATE = "INITIAL";
    public static final String ACTIVE_STATE = "ACTIVE";

    private static final Duration SESSION_DURATION = Duration.ofSeconds(30L);
    private static final Duration DEFAULT_TIMER_DURATION = Duration.ofSeconds(2L);

    private static final int MAX_INCOMING_JOINS = 4;
    private static final int MAX_OUTGOING_JOINS = 3;

    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private AddressCache<A> addressCache;
    private NonceGenerator<byte[]> nonceGenerator;
    private NonceAccessor<byte[]> nonceAccessor;
    private IncomingRequestManager<A, byte[]> incomingRequestManager;
    private OutgoingRequestManager<A, byte[]> outgoingRequestManager;
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
        nonceGenerator = new ByteArrayNonceGenerator(8);
        nonceAccessor = new ByteArrayNonceAccessor();
        incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceAccessor);
        outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceAccessor, endpointDirectory);

        listener.onStarted(selfAddress);

        fsm.switchStateAndProcess(ACTIVE_STATE, instant, new Timer(), null);
    }

    @FilterHandler(ACTIVE_STATE)
    public boolean checkIncomingRequest(String state, FiniteStateMachine fsm, Instant instant, Request message, Endpoint srcEndpoint)
            throws Exception{

        // if message to ourself (or invalid), don't process 
        if (outgoingRequestManager.isMessageTracked(instant, message)) {
            return false;
        }

        // make sure msg is valid and we haven't responded to it yet. if we have, the method being called will return false but will fire
        // off the original response anyway
        return incomingRequestManager.testRequestMessage(instant, message);
    }

    @FilterHandler(ACTIVE_STATE)
    public boolean checkIncomingResponse(String state, FiniteStateMachine fsm, Instant instant, Response message, Endpoint srcEndpoint)
            throws Exception {
        // makes sure msg is valid (false if not) and makes sure this is a response to a message we've sent
        return outgoingRequestManager.testResponseMessage(instant, message);
    }

    @StateHandler(ACTIVE_STATE)
    public void handleTimer(String state, FiniteStateMachine fsm, Instant instant, Timer message, Endpoint srcEndpoint) throws Exception {
        // Prune nonce managers and session managers
        Duration nextIrmDuration = incomingRequestManager.process(instant);
        Duration nextOrmDuration = outgoingRequestManager.process(instant);
        incomingSessions.process(instant);
        outgoingSessions.process(instant);
        Set<A> prunedOutgoingLinks = outgoingSessions.getRemovedIds();

        prunedOutgoingLinks.forEach(prunedAddress -> listener.onDisconnected(selfAddress, prunedAddress));

        // If address cache is at minimum capacity, ask a neighbour for more nodes (if neighbours available)
        if (addressCache.isMinimumCapacity()) {
            List<A> links = getAllSessions();
            if (!links.isEmpty()) {
                // Grab random link from one of our neighbours
                int pos = RandomUtils.nextInt(0, links.size());
                A address = links.get(pos);

                sendQueryRequest(instant, address);
            } else {
                // If we have no neighbours, grab the next node from the address cache and query it
                A next = addressCache.next();
                sendQueryRequest(instant, next);
            }
        }

        // Send a message to all outgoing links telling them we're still alive, otherwise we'll get dropped
        for (A address : outgoingSessions.getSessions()) {
            sendLinkRequest(instant, address);
        }

        // Check to see if we have room for more outgoing links. If we do, sendRequestAndTrack out new link requests
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

        // Reschedule this state to be triggered again
        Duration timerDuration = ProcessableUtils.scheduleEarliestDuration(DEFAULT_TIMER_DURATION, nextOrmDuration, nextIrmDuration);
        endpointScheduler.scheduleMessage(timerDuration, selfEndpoint, selfEndpoint, new Timer());
    }

    @StateHandler(ACTIVE_STATE)
    public void handleLinkRequest(String state, FiniteStateMachine fsm, Instant instant, LinkRequest message, Endpoint srcEndpoint)
            throws Exception {
        A address = endpointIdentifier.identify(srcEndpoint);
        
        boolean successful = incomingSessions.containsSession(address) || incomingSessions.size() < MAX_INCOMING_JOINS;
        sendLinkResponse(instant, srcEndpoint, message, successful);
        
        if (successful) {
            incomingSessions.addOrUpdateSession(instant, SESSION_DURATION, address, null);
        }
    }

    @StateHandler(ACTIVE_STATE)
    public void handleQueryRequest(String state, FiniteStateMachine fsm, Instant instant, QueryRequest message, Endpoint srcEndpoint)
            throws Exception {
        sendQueryResponse(instant, srcEndpoint, message);
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
        // The filter should have checked to make sure that this is a response to a request we sent out
        addressCache.addAll(message.getLinks());
    }

    private List<A> getAllSessions() {
        Set<A> inLinks = incomingSessions.getSessions();
        Set<A> outLinks = outgoingSessions.getSessions();

        List<A> links = new ArrayList<>();
        links.addAll(inLinks);
        links.addAll(outLinks);

        return links;
    }
    
    private void sendLinkRequest(Instant instant, A address) throws Exception {
        outgoingRequestManager.sendRequestAndTrack(instant, new LinkRequest(), address);
    }

    private void sendQueryRequest(Instant instant, A address) throws Exception {
        outgoingRequestManager.sendRequestAndTrack(instant, new QueryRequest(), address);
    }
    
    private void sendLinkResponse(Instant instant, Endpoint srcEndpoint, Object request, boolean successful) throws Exception {
        List<A> links = getAllSessions();
        LinkResponse<A> response = new LinkResponse<>(successful, links);
        incomingRequestManager.sendResponseAndTrack(instant, request, response, srcEndpoint);
    }

    private void sendQueryResponse(Instant instant, Endpoint srcEndpoint, Object request) throws Exception {
        List<A> links = getAllSessions();
        QueryResponse<A> response = new QueryResponse<>(links);
        incomingRequestManager.sendResponseAndTrack(instant, request, response, srcEndpoint);
    }
}
