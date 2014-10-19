package com.offbynull.peernetic.playground.unstructuredmesh;

import com.offbynull.peernetic.playground.unstructuredmesh.AddressCache.RetentionMode;
import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.message.Nonce;
import com.offbynull.peernetic.common.message.NonceAccessor;
import com.offbynull.peernetic.common.message.NonceGenerator;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.javaflow.BaseJavaflowTask;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.external.LinkRequest;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.external.LinkResponse;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.external.QueryRequest;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.external.QueryResponse;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.internal.Start;
import com.offbynull.peernetic.playground.unstructuredmesh.messages.internal.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.javaflow.Continuation;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.Validate;

public final class UnstructuredClient<A> extends BaseJavaflowTask {

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
    private SessionManager<A> incomingSessions;
    private SessionManager<A> outgoingSessions;
    private Endpoint selfEndpoint;
    private A selfAddress;

    UnstructuredClientListener<A> listener;

    public UnstructuredClient(UnstructuredClientListener<A> listener) {
        Validate.notNull(listener);
        this.listener = listener;
    }

    
    @Override
    public void run() {
        try {
            Start<A> start = (Start<A>) getMessage();

            endpointDirectory = start.getEndpointDirectory();
            endpointIdentifier = start.getEndpointIdentifier();
            endpointScheduler = start.getEndpointScheduler();
            selfEndpoint = start.getSelfEndpoint();
            selfAddress = start.getSelfAddress();
            addressCache = new AddressCache<>(1, 256, start.getBootstrapAddresses(), RetentionMode.RETAIN_OLDEST);
            incomingSessions = new SessionManager<>();
            outgoingSessions = new SessionManager<>();
            nonceGenerator = new ByteArrayNonceGenerator(8);
            nonceAccessor = new ByteArrayNonceAccessor();

            listener.onStarted(selfAddress);


            handleTimer(getTime()); // initial call to start things off


            while (true) {
                Continuation.suspend();
                if (getMessage() instanceof Request) {
                    if (getMessage() instanceof LinkRequest) {
                        handleLinkRequest(getTime(), (LinkRequest) getMessage(), getSource());
                    } else if (getMessage() instanceof QueryRequest) {
                        handleQueryRequest(getTime(), (QueryRequest) getMessage(), getSource());
                    }
                } else if (getMessage() instanceof Response) {
                    if (getMessage() instanceof LinkResponse) {
                        handleLinkResponse(getTime(), (LinkResponse) getMessage(), getSource());
                    } else if (getMessage() instanceof QueryResponse) {
                        handleQueryResponse(getTime(), (QueryResponse) getMessage(), getSource());
                    }
                } else if (getMessage() instanceof Timer) {
                    handleTimer(getTime());
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    private void handleTimer(Instant time) throws Exception {
        // Prune nonce managers and session managers
        incomingSessions.process(time);
        outgoingSessions.process(time);
        Set<A> prunedOutgoingLinks = outgoingSessions.getRemovedIds();

        prunedOutgoingLinks.forEach(prunedAddress -> listener.onDisconnected(selfAddress, prunedAddress));

        // If address cache is at minimum capacity, ask a neighbour for more nodes (if neighbours available)
        if (addressCache.isMinimumCapacity()) {
            List<A> links = getAllSessions();
            if (!links.isEmpty()) {
                // Grab random link from one of our neighbours
                int pos = RandomUtils.nextInt(0, links.size());
                A address = links.get(pos);

                sendQueryRequest(address);
            } else {
                // If we have no neighbours, grab the next node from the address cache and query it
                A next = addressCache.next();
                sendQueryRequest(next);
            }
        }

        // Send a message to all outgoing links telling them we're still alive, otherwise we'll get dropped
        for (A address : outgoingSessions.getSessions()) {
            sendLinkRequest(address);
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

            sendLinkRequest(address);
        }

        // Reschedule this state to be triggered again
        endpointScheduler.scheduleMessage(DEFAULT_TIMER_DURATION, selfEndpoint, selfEndpoint, new Timer());
    }

    public void handleLinkRequest(Instant time, LinkRequest message, Endpoint srcEndpoint) throws Exception {
        A address = endpointIdentifier.identify(srcEndpoint);
        
        boolean successful = incomingSessions.containsSession(address) || incomingSessions.size() < MAX_INCOMING_JOINS;
        sendLinkResponse(srcEndpoint, message, successful);
        
        if (successful) {
            incomingSessions.addOrUpdateSession(time, SESSION_DURATION, address, null);
        }
    }

    public void handleQueryRequest(Instant time, QueryRequest message, Endpoint srcEndpoint) throws Exception {
        sendQueryResponse(srcEndpoint, message);
    }

    public void handleLinkResponse(Instant time, LinkResponse message, Endpoint srcEndpoint) {
        // The filter should have checked to make sure that this is a response to a rquest we sent out
        A dstAddress = endpointIdentifier.identify(srcEndpoint);

        boolean alreadyExists = outgoingSessions.containsSession(dstAddress);
        if (message.isSuccessful()) {
            if (!alreadyExists) { // If new connection
                if (outgoingSessions.size() < MAX_OUTGOING_JOINS) { // If space is available for new connection
                    outgoingSessions.addOrUpdateSession(time, SESSION_DURATION, dstAddress, null);
                    listener.onOutgoingConnected(selfAddress, dstAddress);
                }
            } else { // If existing connection
                outgoingSessions.addOrUpdateSession(time, SESSION_DURATION, dstAddress, null);
            }
        }

        addressCache.addAll(message.getLinks());
    }

    public void handleQueryResponse(Instant time, QueryResponse message, Endpoint srcEndpoint) {
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
    
    private void sendLinkRequest(A address) throws Exception {
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        
        LinkRequest req = new LinkRequest();
        Nonce<byte[]> nonce = nonceGenerator.generate();
        nonceAccessor.set(req, nonce);
        
        dstEndpoint.send(selfEndpoint, req);
    }

    private void sendQueryRequest(A address) throws Exception {
        Endpoint dstEndpoint = endpointDirectory.lookup(address);
        
        QueryRequest req = new QueryRequest();
        Nonce<byte[]> nonce = nonceGenerator.generate();
        nonceAccessor.set(req, nonce);
        
        dstEndpoint.send(selfEndpoint, req);
    }
    
    private void sendLinkResponse(Endpoint srcEndpoint, Object request, boolean successful) throws Exception {
        List<A> links = getAllSessions();
        
        LinkResponse<A> resp = new LinkResponse<>(successful, links);
        Nonce<byte[]> nonce = nonceAccessor.get(request);
        nonceAccessor.set(resp, nonce);
        
        srcEndpoint.send(selfEndpoint, resp);
    }

    private void sendQueryResponse(Endpoint srcEndpoint, Object request) throws Exception {
        List<A> links = getAllSessions();
        
        QueryResponse<A> resp = new QueryResponse<>(links);
        Nonce<byte[]> nonce = nonceAccessor.get(request);
        nonceAccessor.set(resp, nonce);
        
        srcEndpoint.send(selfEndpoint, resp);
    }
}
