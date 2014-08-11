package com.offbynull.peernetic.demos.chord;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.ByteArrayNonceWrapper;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.IncomingRequestManager;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Request;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.chord.ChordActiveListener.Mode;
import com.offbynull.peernetic.demos.chord.core.ChordState;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import com.offbynull.peernetic.demos.chord.core.FingerTable;
import com.offbynull.peernetic.demos.chord.core.InternalPointer;
import com.offbynull.peernetic.demos.chord.core.Pointer;
import com.offbynull.peernetic.demos.chord.fsms.CheckPredecessor;
import com.offbynull.peernetic.demos.chord.fsms.FixFinger;
import com.offbynull.peernetic.demos.chord.fsms.InitFingerTable;
import com.offbynull.peernetic.demos.chord.fsms.Stabilize;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetIdResponse;
import com.offbynull.peernetic.demos.chord.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.demos.chord.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.demos.chord.messages.external.NotifyRequest;
import com.offbynull.peernetic.demos.chord.messages.external.NotifyResponse;
import com.offbynull.peernetic.demos.chord.messages.internal.Start;
import com.offbynull.peernetic.demos.unstructured.messages.internal.Timer;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ChordClient<A> {

    public static final String INITIAL_STATE = "start";
    public static final String INITIAL_POPULATE_FINGERS_STATE = "init_fix_fingers";
    public static final String INITIAL_STABILIZE_STATE = "init_stabilize";
    public static final String JOINED_STATE = "joined";
    
    private static final Duration TIMER_DURATION = Duration.ofSeconds(2L);

    private final ChordActiveListener<Id> activeListener;
    private final ChordLinkListener<Id> linkListener;
    private final ChordUnlinkListener<Id> unlinkListener;

    private EndpointDirectory<A> endpointDirectory;
    private EndpointIdentifier<A> endpointIdentifier;
    private EndpointScheduler endpointScheduler;
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

    private FixFinger<A> fixFinger;
    private FiniteStateMachine<Endpoint> fixFingerFsm;

    private CheckPredecessor<A> checkPredecessor;
    private FiniteStateMachine<Endpoint> checkPredecessorFsm;

    public ChordClient(ChordActiveListener<Id> activeListener, ChordLinkListener<Id> linkListener, ChordUnlinkListener<Id> unlinkListener) {
        Validate.notNull(activeListener);
        Validate.notNull(linkListener);
        Validate.notNull(unlinkListener);
        this.activeListener = activeListener;
        this.linkListener = linkListener;
        this.unlinkListener = unlinkListener;
    }

    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Start<A> message, Endpoint srcEndpoint) {
        endpointDirectory = message.getEndpointDirectory();
        endpointIdentifier = message.getEndpointIdentifier();
        endpointScheduler = message.getEndpointScheduler();
        selfEndpoint = message.getSelfEndpoint();
        selfId = message.getSelfId();
        bootstrapAddress = message.getBootstrapAddress();
        nonceGenerator = new ByteArrayNonceGenerator(8);
        nonceWrapper = new ByteArrayNonceWrapper();
        incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceWrapper);
        outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);

        chordState = new ChordState<>(new InternalPointer(selfId));

        if (bootstrapAddress != null) {
            initFingerTable = new InitFingerTable<>(selfId, bootstrapAddress, endpointDirectory, endpointIdentifier, endpointScheduler,
                    selfEndpoint, nonceGenerator, nonceWrapper);
            initFingerTableFsm = new FiniteStateMachine(initFingerTable, InitFingerTable.INITIAL_STATE, Endpoint.class);
            initFingerTableFsm.process(instant, new Object(), srcEndpoint);

            fsm.setState(INITIAL_POPULATE_FINGERS_STATE);
        } else {
            activeListener.active(selfId, Mode.SEED);
            startOrCheckMaintenance(instant, message, selfEndpoint);
            endpointScheduler.scheduleMessage(TIMER_DURATION, srcEndpoint, srcEndpoint, new Timer());
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
                    .forEach(x -> {
                        chordState.putFinger((ExternalPointer<A>) x);
                        notifyStateChange();
                    });

            stabilize = new Stabilize<>(selfId, (ExternalPointer<A>) successor, endpointDirectory, endpointScheduler, selfEndpoint,
                    nonceGenerator, nonceWrapper);
            stabilizeFsm = new FiniteStateMachine<>(stabilize, Stabilize.INITIAL_STATE, Endpoint.class);
            stabilizeFsm.process(instant, new Object(), srcEndpoint);
            fsm.setState(INITIAL_STABILIZE_STATE);
        }
    }

    @StateHandler(INITIAL_STABILIZE_STATE)
    public void handleStabilizing(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint) {
        stabilizeFsm.process(instant, message, srcEndpoint);

        if (stabilizeFsm.getState().equals(Stabilize.DONE_STATE)) {
            activeListener.active(selfId, Mode.JOIN);
            
            Pointer successor = stabilize.getNewSuccessor();

            if (successor instanceof ExternalPointer) {
                chordState.setSuccessor((ExternalPointer<A>) successor, Collections.EMPTY_LIST);
                notifyStateChange();
            }

            stabilizeFsm = null; // dont' want to call process again in call below
            startOrCheckMaintenance(instant, message, srcEndpoint);

            endpointScheduler.scheduleMessage(TIMER_DURATION, srcEndpoint, srcEndpoint, new Timer());
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
    public void handleGetIdRequest(String state, FiniteStateMachine fsm, Instant instant, GetIdRequest request, Endpoint srcEndpoint)
            throws Exception {
        incomingRequestManager.sendResponseAndTrack(instant, request, new GetIdResponse(selfId.getValueAsByteArray()), srcEndpoint);
    }

    @StateHandler(JOINED_STATE)
    public void handleGetClosestPrecedingFingerRequest(String state, FiniteStateMachine fsm, Instant instant,
            GetClosestPrecedingFingerRequest request, Endpoint srcEndpoint) throws Exception {
        Id id = new Id(request.getId(), selfId.getLimitAsByteArray());
        Pointer pointer = chordState.getClosestPreceding(id);
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        incomingRequestManager.sendResponseAndTrack(instant, request,
                new GetClosestPrecedingFingerResponse<>(msgValues.getLeft(), msgValues.getRight()), srcEndpoint);
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
            GetSuccessorRequest request, Endpoint srcEndpoint) throws Exception {
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

        ExternalPointer<A> newPredecessor = new ExternalPointer<>(id, endpointIdentifier.identify(srcEndpoint));
        ExternalPointer<A> existingPredecessor = (ExternalPointer<A>) chordState.getPredecessor();
        if (chordState.getPredecessor() == null || id.isWithin(existingPredecessor.getId(), true, selfId, false)) {
            chordState.setPredecessor(newPredecessor);
            notifyStateChange();
        }
        
        Pointer pointer = chordState.getPredecessor();
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        incomingRequestManager.sendResponseAndTrack(instant, request,
                new NotifyResponse<>(msgValues.getLeft(), msgValues.getRight()), srcEndpoint);
    }

    private ImmutablePair<byte[], A> convertPointerToMessageDetails(Pointer pointer) {
        if (pointer == null) {
            return new ImmutablePair<>(null, null);
        }

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

    @StateHandler(JOINED_STATE)
    public void handleTimer(String state, FiniteStateMachine fsm, Instant instant, Timer message, Endpoint srcEndpoint) {
        startOrCheckMaintenance(instant, message, srcEndpoint);
        endpointScheduler.scheduleMessage(TIMER_DURATION, srcEndpoint, srcEndpoint, message);
    }

    @StateHandler(JOINED_STATE)
    public void handleJoinedFallthrough(String state, FiniteStateMachine fsm, Instant instant, Object message, Endpoint srcEndpoint) {
        startOrCheckMaintenance(instant, message, srcEndpoint);
    }

    private void startOrCheckMaintenance(Instant instant, Object message, Endpoint srcEndpoint) {
        if (stabilizeFsm != null) {
            stabilizeFsm.process(instant, message, srcEndpoint);
        }
        if (stabilizeFsm == null || stabilizeFsm.getState().equals(Stabilize.DONE_STATE)) {
            Pointer successor;
            if (stabilizeFsm != null) {
                successor = stabilize.getNewSuccessor();
                if (successor instanceof ExternalPointer) {
                    chordState.setSuccessor((ExternalPointer<A>) successor, Collections.EMPTY_LIST);
                    notifyStateChange();
                } else {
                    chordState.setSuccessor(new InternalPointer(selfId), Collections.EMPTY_LIST);
                    notifyStateChange();
                }
            } else {
                successor = chordState.getSuccessor();
            }

            if (successor instanceof ExternalPointer) {
                stabilize = new Stabilize<>(selfId, (ExternalPointer<A>) successor, endpointDirectory, endpointScheduler, selfEndpoint,
                        nonceGenerator, nonceWrapper);
                stabilizeFsm = new FiniteStateMachine<>(stabilize, Stabilize.INITIAL_STATE, Endpoint.class);
                stabilizeFsm.process(instant, new Object(), srcEndpoint);
            }
        }

        if (fixFingerFsm != null) {
            fixFingerFsm.process(instant, message, srcEndpoint);
        }
        if (fixFingerFsm == null || fixFingerFsm.getState().equals(FixFinger.DONE_STATE)) {
            if (fixFinger != null) {
                int idx = fixFinger.getIndex();
                Pointer pointer = fixFinger.getNewFinger();

                if (pointer != null) {
                    if (pointer instanceof InternalPointer) {
                        Pointer existingFinger = chordState.getFinger(idx);
                        if (existingFinger instanceof ExternalPointer) {
                            chordState.removeFinger((ExternalPointer<A>) existingFinger);
                            notifyStateChange();
                        }
                    } else if (pointer instanceof ExternalPointer) {
                        chordState.putFinger((ExternalPointer<A>) pointer);
                        notifyStateChange();
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }

            fixFinger = new FixFinger<>(selfId, chordState.getFingerTable(), endpointDirectory, endpointIdentifier, endpointScheduler,
                    selfEndpoint, nonceGenerator, nonceWrapper);
            fixFingerFsm = new FiniteStateMachine<>(fixFinger, FixFinger.INITIAL_STATE, Endpoint.class);
            fixFingerFsm.process(instant, new Object(), srcEndpoint);
        }

        if (checkPredecessorFsm != null) {
            checkPredecessorFsm.process(instant, message, srcEndpoint);
        }
        if (checkPredecessorFsm == null || checkPredecessorFsm.getState().equals(CheckPredecessor.DONE_STATE)) {
            if (checkPredecessor != null) {
                if (checkPredecessor.isPredecessorUnresponsive()) {
                    chordState.removePredecessor();
                    notifyStateChange();
                }
            }

            checkPredecessor = new CheckPredecessor<>(selfId, (ExternalPointer<A>) chordState.getPredecessor(), endpointDirectory,
                    endpointScheduler, selfEndpoint, nonceGenerator, nonceWrapper);
            checkPredecessorFsm = new FiniteStateMachine<>(checkPredecessor, CheckPredecessor.INITIAL_STATE, Endpoint.class);
            checkPredecessorFsm.process(instant, new Object(), srcEndpoint);
        }
    }
    
    private List<Pointer> oldPointers = new ArrayList<>();
    private void notifyStateChange() {
        List<Pointer> newPointers = new ArrayList<>(Arrays.<Pointer>asList(
                chordState.dumpFingerTable().stream().filter(x -> x instanceof ExternalPointer).toArray(x -> new Pointer[x])));
        
        List<Pointer> removedPointers = new ArrayList<>(oldPointers);
        removedPointers.removeAll(newPointers);
        removedPointers.forEach(x -> unlinkListener.unlinked(selfId, x.getId()));
        
        List<Pointer> addedPointers = new ArrayList<>(newPointers);
        addedPointers.removeAll(oldPointers);
        addedPointers.forEach(x -> linkListener.linked(selfId, x.getId()));
        
        oldPointers = newPointers;
    }
}
