package com.offbynull.peernetic.demos.chord;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.AddressCache;
import com.offbynull.peernetic.common.AddressCache.RetentionMode;
import com.offbynull.peernetic.common.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.ByteArrayNonceWrapper;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.IncomingRequestManager;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Request;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.chord.core.ChordState;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.core.FingerTable;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.demos.chord.fsms.InitFingerTable;
import com.offbynull.peernetic.demos.chord.fsms.Stabilize;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.demos.chord.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.demos.chord.messages.external.NotifyRequest;
import com.offbynull.peernetic.demos.chord.messages.internal.Start;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Instant;
import java.util.Collections;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ChordClient<A> {
    public static final String INITIAL_STATE = "start";
    public static final String INITIAL_POPULATE_FINGERS_STATE = "initialize";
    public static final String INITIAL_STABILIZE_STATE = "joining";
    public static final String JOINED_STATE = "joined";

    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
    private AddressCache<A> addressCache;
    private NonceGenerator<byte[]> nonceGenerator;
    private NonceWrapper<byte[]> nonceWrapper;
    private IncomingRequestManager<A, byte[]> incomingRequestManager;
    private OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private Endpoint selfEndpoint;
    
    private Id selfId;
    private A bootstrapAddress;
    
    private ChordState<A> chordState;
    
    private InitFingerTable<A> initFingerTable;
    private FiniteStateMachine<Endpoint> initFingerTableFsm;
    
    private Stabilize<A> stabilize;
    private FiniteStateMachine<Endpoint> stabilizeFsm;

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Start<A> message, Endpoint srcEndpoint) {
        endpointDirectory = message.getEndpointDirectory();
        endpointIdentifier = message.getEndpointIdentifier();
        endpointScheduler = message.getEndpointScheduler();
        selfEndpoint = message.getSelfEndpoint();
        selfId = message.getSelfId();
        bootstrapAddress = message.getBootstrapAddress();
        addressCache = new AddressCache<>(1, 256, Collections.singleton(bootstrapAddress), RetentionMode.RETAIN_OLDEST);
        nonceGenerator = new ByteArrayNonceGenerator(8);
        nonceWrapper = new ByteArrayNonceWrapper();
        incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceWrapper);
        outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);
        
        chordState = new ChordState<>(new InternalPointer(selfId));

        if (bootstrapAddress == null) {
            initFingerTable = new InitFingerTable<>(selfId, bootstrapAddress, endpointDirectory, endpointScheduler, selfEndpoint,
                    nonceGenerator, nonceWrapper);
            initFingerTableFsm = new FiniteStateMachine(initFingerTable, InitFingerTable.INITIAL_STATE, Endpoint.class);
            initFingerTableFsm.process(instant, new Object(), srcEndpoint);
            
            fsm.setState(INITIAL_POPULATE_FINGERS_STATE);
        } else {
            fsm.setState(JOINED_STATE);
        }
    }

    @StateHandler(INITIAL_POPULATE_FINGERS_STATE)
    public void handleInitialize(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint) {
        initFingerTableFsm.process(instant, message, srcEndpoint);
        
        if (initFingerTableFsm.getState().equals(InitFingerTable.DONE_STATE)) {
            FingerTable<A> fingerTable = initFingerTable.getFingerTable();
            Pointer successor = fingerTable.get(0);
            
            if (successor instanceof InternalPointer) {
                throw new IllegalStateException("Join failed");
            }
            
            // put external pointer's in to chordState
            fingerTable.dump().stream()
                    .filter(x -> x instanceof ExternalPointer)
                    .forEach(x -> chordState.putFinger((ExternalPointer<A>)x));
            
            stabilize = new Stabilize<>(selfId, (ExternalPointer<A>) successor, endpointDirectory, endpointScheduler, selfEndpoint,
                    nonceGenerator, nonceWrapper);
            stabilizeFsm = new FiniteStateMachine<>(stabilize, Stabilize.INITIAL_STATE, Endpoint.class);
            fsm.setState(INITIAL_STABILIZE_STATE);
        }
    }

    @StateHandler(INITIAL_STABILIZE_STATE)
    public void handleStabilizing(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint) {
        stabilizeFsm.process(instant, message, srcEndpoint);
        
        if (stabilizeFsm.getState().equals(Stabilize.DONE_STATE)) {
            Pointer successor = stabilize.getResult();
            
            if (successor instanceof ExternalPointer) {
                chordState.setSuccessor((ExternalPointer<A>) successor, Collections.EMPTY_LIST);
            }

            fsm.setState(JOINED_STATE);
        }
    }

    @FilterHandler(JOINED_STATE)
    public boolean filterRequests(String state, FiniteStateMachine fsm, Instant instant, Request request, Endpoint srcEndpoint)
            throws Exception {
        return !outgoingRequestManager.isRequestTracked(instant, request) && incomingRequestManager.testRequestMessage(instant, request);
    }
    
    @FilterHandler(JOINED_STATE)
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response, Endpoint srcEndpoint)
            throws Exception {
        return outgoingRequestManager.testResponseMessage(instant, response);
    }

    @StateHandler(JOINED_STATE)
    public void handleGetClosestPrecedingFingerRequest(String state, FiniteStateMachine fsm, Instant instant,
            GetClosestPrecedingFingerRequest request, Endpoint srcEndpoint) throws Exception {
        Id id = new Id(request.getId(), selfId.getLimitAsByteArray());
        Pointer pointer = chordState.getClosestPreceding(id);
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        incomingRequestManager.sendResponseAndTrack(instant, request,
                    new GetPredecessorResponse<>(msgValues.getLeft(), msgValues.getRight()), srcEndpoint);
    }
    
    @StateHandler(JOINED_STATE)
    public void handleGetPredecessorRequest(String state, FiniteStateMachine fsm, Instant instant,
            GetPredecessorRequest request, Endpoint srcEndpoint) throws Exception {
        Pointer pointer = chordState.getPredecessor();
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        incomingRequestManager.sendResponseAndTrack(instant, request,
                    new GetPredecessorResponse<>(msgValues.getLeft(), msgValues.getRight()), srcEndpoint);
    }

    @StateHandler(JOINED_STATE)
    public void handleGetSuccessorRequest(String state, FiniteStateMachine fsm, Instant instant,
            GetPredecessorRequest request, Endpoint srcEndpoint) throws Exception {
        Pointer pointer = chordState.getSuccessor();
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        incomingRequestManager.sendResponseAndTrack(instant, request,
                    new GetSuccessorResponse<>(msgValues.getLeft(), msgValues.getRight()), srcEndpoint);
    }

    @StateHandler(JOINED_STATE)
    public void handleNotifyRequest(String state, FiniteStateMachine fsm, Instant instant,
            NotifyRequest request, Endpoint srcEndpoint) throws Exception {
        byte[] idBytes = request.getId();
        Id id = new Id(idBytes, selfId.getLimitAsByteArray());
        
        ExternalPointer<A> externalPointer = new ExternalPointer<>(id, endpointIdentifier.identify(srcEndpoint));
        chordState.setPredecessor(externalPointer);
    }
    
    private ImmutablePair<byte[], A> convertPointerToMessageDetails(Pointer pointer) {
        byte[] idBytes = pointer.getId().getValueAsByteArray();
        if (pointer instanceof InternalPointer) {
            return new ImmutablePair<>(idBytes, null);
        } else if (pointer instanceof ExternalPointer) {
            A address = ((ExternalPointer<A>) pointer).getAddress();
            return new ImmutablePair<>(idBytes, address);
        } else {
            throw new IllegalStateException();
        }        
    }

    TODO: NEED TO ADD PERIODIC FIX FINGER, PERIODIC CHECK PREDECESSOR (see old CheckPredecessorTask), AND PERIODIC STABILIZE.;
    @StateHandler(JOINED_STATE)
    public void handleJoinedFallthrough(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint) {
        // SEND
    }
}
