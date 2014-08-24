package com.offbynull.peernetic.demos.chord;

import com.offbynull.peernetic.common.message.ByteArrayNonceGenerator;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.message.ByteArrayNonceAccessor;
import com.offbynull.peernetic.common.transmission.IncomingRequestManager;
import com.offbynull.peernetic.common.message.Message;
import com.offbynull.peernetic.common.transmission.OutgoingRequestManager;
import com.offbynull.peernetic.common.message.Request;
import com.offbynull.peernetic.common.message.Response;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ChordClient<A> {

    public static final String INITIAL_STATE = "start";
    public static final String INITIAL_POPULATE_FINGERS_STATE = "init_fix_fingers";
    public static final String INITIAL_STABILIZE_STATE = "init_stabilize";
    public static final String JOINED_STATE = "joined";
    
    private static final Duration TIMER_DURATION = Duration.ofSeconds(2L);

    private InitFingerTable<A> initFingerTable;
    private FiniteStateMachine<ChordContext<A>> initFingerTableFsm;

    private Stabilize<A> stabilize;
    private FiniteStateMachine<ChordContext<A>> stabilizeFsm;

    private FixFinger<A> fixFinger;
    private FiniteStateMachine<ChordContext<A>> fixFingerFsm;

    private CheckPredecessor<A> checkPredecessor;
    private FiniteStateMachine<ChordContext<A>> checkPredecessorFsm;

    @StateHandler(INITIAL_STATE)
    public void handleStart(FiniteStateMachine fsm, Instant time, Start<A> message, ChordContext<A> context) {
        context.setEndpointDirectory(message.getEndpointDirectory());
        context.setEndpointIdentifier(message.getEndpointIdentifier());
        context.setEndpointScheduler(message.getEndpointScheduler());
        context.setSelfEndpoint(message.getSelfEndpoint());
        context.setSelfId(message.getSelfId());
        context.setBootstrapAddress(message.getBootstrapAddress());
        context.setNonceGenerator(new ByteArrayNonceGenerator(8));
        context.setNonceAccessor(new ByteArrayNonceAccessor());
        
        context.setIncomingRequestManager(new IncomingRequestManager<>(context.getSelfEndpoint(), context.getNonceAccessor()));
        context.setOutgoingRequestManager(new OutgoingRequestManager<>(context.getSelfEndpoint(), context.getNonceGenerator(),
                context.getNonceAccessor(), context.getEndpointDirectory()));

        context.setChordState(new ChordState<>(new InternalPointer(context.getSelfId())));

        if (context.getBootstrapAddress() != null) {
            initFingerTable = new InitFingerTable<>();
            initFingerTableFsm = new FiniteStateMachine(initFingerTable, InitFingerTable.INITIAL_STATE, ChordContext.class);
            initFingerTableFsm.process(time, new Object(), context);

            fsm.setState(INITIAL_POPULATE_FINGERS_STATE);
        } else {
            context.getActiveListener().active(context.getSelfId(), Mode.SEED);
            startOrCheckMaintenance(time, message, context);
            context.getEndpointScheduler().scheduleMessage(TIMER_DURATION, context.getSelfEndpoint(), context.getSelfEndpoint(),
                    new Timer());
            fsm.setState(JOINED_STATE);
        }
    }

    @StateHandler(INITIAL_POPULATE_FINGERS_STATE)
    public void handleInitialize(FiniteStateMachine fsm, Instant time, Object message, ChordContext<A> context) {
        initFingerTableFsm.process(time, message, context);

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
                        context.getChordState().putFinger((ExternalPointer<A>) x);
                        notifyStateChange(context);
                    });

            stabilize = new Stabilize();
            stabilizeFsm = new FiniteStateMachine(stabilize, Stabilize.INITIAL_STATE, ChordContext.class);
            stabilizeFsm.process(time, new Object(), context);
            fsm.setState(INITIAL_STABILIZE_STATE);
        }
    }

    @StateHandler(INITIAL_STABILIZE_STATE)
    public void handleStabilizing(FiniteStateMachine fsm, Instant time, Object message, ChordContext<A> context) {
        stabilizeFsm.process(time, message, context);

        if (stabilizeFsm.getState().equals(Stabilize.DONE_STATE)) {
            context.getActiveListener().active(context.getSelfId(), Mode.JOIN);
            
            Pointer successor = stabilize.getNewSuccessor();

            if (successor instanceof ExternalPointer) {
                context.getChordState().setSuccessor((ExternalPointer<A>) successor, Collections.EMPTY_LIST);
                notifyStateChange(context);
            }

            stabilizeFsm = null; // dont' want to call process again in call below
            startOrCheckMaintenance(time, message, context);

            context.getEndpointScheduler().scheduleMessage(TIMER_DURATION, context.getSelfEndpoint(), context.getSelfEndpoint(),
                    new Timer());
            fsm.setState(JOINED_STATE);
        }
    }

    @FilterHandler(JOINED_STATE)
    public boolean filterRequests(FiniteStateMachine fsm, Instant time, Request request, ChordContext<A> context)
            throws Exception {
        return !context.getOutgoingRequestManager().isTrackedRequest(time, request)
                && context.getIncomingRequestManager().testRequestMessage(time, request);
    }

    @FilterHandler(JOINED_STATE)
    public boolean filterResponses(FiniteStateMachine fsm, Instant time, Response response, ChordContext<A> context)
            throws Exception {
        return context.getOutgoingRequestManager().isExpectedResponse(time, response);
    }

    @StateHandler(JOINED_STATE)
    public void handleGetIdRequest(FiniteStateMachine fsm, Instant time, GetIdRequest request, ChordContext<A> context)
            throws Exception {
        context.getIncomingRequestManager().sendResponseAndTrack(time, request,
                new GetIdResponse(context.getSelfId().getValueAsByteArray()), context.getSourceEndpoint());
    }

    @StateHandler(JOINED_STATE)
    public void handleGetClosestPrecedingFingerRequest(FiniteStateMachine fsm, Instant time,
            GetClosestPrecedingFingerRequest request, ChordContext<A> context) throws Exception {
        Id id = new Id(request.getId(), context.getSelfId().getLimitAsByteArray());
        Pointer pointer = context.getChordState().getClosestPreceding(id);
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        context.getIncomingRequestManager().sendResponseAndTrack(time, request,
                new GetClosestPrecedingFingerResponse<>(msgValues.getLeft(), msgValues.getRight()), context.getSourceEndpoint());
    }

    @StateHandler(JOINED_STATE)
    public void handleGetPredecessorRequest(FiniteStateMachine fsm, Instant time,
            GetPredecessorRequest request, ChordContext<A> context) throws Exception {
        Pointer pointer = context.getChordState().getPredecessor();
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        context.getIncomingRequestManager().sendResponseAndTrack(time, request,
                new GetPredecessorResponse<>(msgValues.getLeft(), msgValues.getRight()), context.getSourceEndpoint());
    }

    @StateHandler(JOINED_STATE)
    public void handleGetSuccessorRequest(FiniteStateMachine fsm, Instant time,
            GetSuccessorRequest request, ChordContext<A> context) throws Exception {
        Pointer pointer = context.getChordState().getSuccessor();
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        context.getIncomingRequestManager().sendResponseAndTrack(time, request,
                new GetSuccessorResponse<>(msgValues.getLeft(), msgValues.getRight()), context.getSourceEndpoint());
    }

    @StateHandler(JOINED_STATE)
    public void handleNotifyRequest(FiniteStateMachine fsm, Instant time,
            NotifyRequest request, ChordContext<A> context) throws Exception {
        byte[] idBytes = request.getId();
        Id id = new Id(idBytes, context.getSelfId().getLimitAsByteArray());

        ExternalPointer<A> newPredecessor =
                new ExternalPointer<>(id, context.getEndpointIdentifier().identify(context.getSourceEndpoint()));
        ExternalPointer<A> existingPredecessor = (ExternalPointer<A>) context.getChordState().getPredecessor();
        if (context.getChordState().getPredecessor() == null || id.isWithin(existingPredecessor.getId(), true, context.getSelfId(), false)) {
            context.getChordState().setPredecessor(newPredecessor);
            notifyStateChange(context);
        }
        
        Pointer pointer = context.getChordState().getPredecessor();
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(pointer);
        context.getIncomingRequestManager().sendResponseAndTrack(time, request,
                new NotifyResponse<>(msgValues.getLeft(), msgValues.getRight()), context.getSourceEndpoint());
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
    public void handleTimer(FiniteStateMachine fsm, Instant time, Timer message, ChordContext<A> context) {
        startOrCheckMaintenance(time, message, context);
        context.getEndpointScheduler().scheduleMessage(TIMER_DURATION, context.getSelfEndpoint(), context.getSelfEndpoint(), message);
    }

    @StateHandler(JOINED_STATE)
    public void handleJoinedFallthrough(FiniteStateMachine fsm, Instant time, Object message, ChordContext<A> context)
            throws Exception {
        startOrCheckMaintenance(time, message, context);
        
        if (message instanceof Message) {
            context.getOutgoingRequestManager().testResponseMessage(time, message);
        }
    }

    private void startOrCheckMaintenance(Instant time, Object message, ChordContext<A> context) {
        if (stabilizeFsm != null) {
            stabilizeFsm.process(time, message, context);
        }
        if (stabilizeFsm == null || stabilizeFsm.getState().equals(Stabilize.DONE_STATE)) {
            Pointer successor;
            if (stabilizeFsm != null) {
                successor = stabilize.getNewSuccessor();
                if (successor instanceof ExternalPointer) {
                    context.getChordState().setSuccessor((ExternalPointer<A>) successor, Collections.EMPTY_LIST);
                    notifyStateChange(context);
                } else {
                    context.getChordState().setSuccessor(new InternalPointer(context.getSelfId()), Collections.EMPTY_LIST);
                    notifyStateChange(context);
                }
            } else {
                successor = context.getChordState().getSuccessor();
            }

            if (successor instanceof ExternalPointer) {
                stabilize = new Stabilize();
                stabilizeFsm = new FiniteStateMachine(stabilize, Stabilize.INITIAL_STATE, ChordContext.class);
                stabilizeFsm.process(time, new Object(), context);
            }
        }

        if (fixFingerFsm != null) {
            fixFingerFsm.process(time, message, context);
        }
        if (fixFingerFsm == null || fixFingerFsm.getState().equals(FixFinger.DONE_STATE)) {
            if (fixFinger != null) {
                int idx = fixFinger.getIndex();
                Pointer pointer = fixFinger.getNewFinger();

                if (pointer != null) {
                    if (pointer instanceof InternalPointer) {
                        Pointer existingFinger = context.getChordState().getFinger(idx);
                        if (existingFinger instanceof ExternalPointer) {
                            context.getChordState().removeFinger((ExternalPointer<A>) existingFinger);
                            notifyStateChange(context);
                        }
                    } else if (pointer instanceof ExternalPointer) {
                        context.getChordState().putFinger((ExternalPointer<A>) pointer);
                        notifyStateChange(context);
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }

            fixFinger = new FixFinger<>();
            fixFingerFsm = new FiniteStateMachine(fixFinger, FixFinger.INITIAL_STATE, ChordContext.class);
            fixFingerFsm.process(time, new Object(), context);
        }

        if (checkPredecessorFsm != null) {
            checkPredecessorFsm.process(time, message, context);
        }
        if (checkPredecessorFsm == null || checkPredecessorFsm.getState().equals(CheckPredecessor.DONE_STATE)) {
            if (checkPredecessor != null) {
                // if predecessor is unresponsive, and the predecessor hasn't changed from when we started the check predecessor task
                if (checkPredecessor.isPredecessorUnresponsive()
                        && Objects.equals(checkPredecessor.getExistingPredecessor(), context.getChordState().getPredecessor())) {
                    context.getChordState().removePredecessor();
                    notifyStateChange(context);
                }
            }

            checkPredecessor = new CheckPredecessor<>();
            checkPredecessorFsm = new FiniteStateMachine(checkPredecessor, CheckPredecessor.INITIAL_STATE, ChordContext.class);
            checkPredecessorFsm.process(time, new Object(), context);
        }
    }
    
    private Set<Pointer> lastNotifiedPointers = new HashSet<>();
    private void notifyStateChange(ChordContext<A> context) {
        Set<Pointer> newPointers = new HashSet<>(Arrays.<Pointer>asList(
                context.getChordState().dumpFingerTable().stream().filter(x -> x instanceof ExternalPointer).toArray(x -> new Pointer[x])));
        
        Set<Pointer> addedPointers = new HashSet<>(newPointers);
        addedPointers.removeAll(lastNotifiedPointers);
        addedPointers.forEach(x -> context.getLinkListener().linked(context.getSelfId(), x.getId()));

        Set<Pointer> removedPointers = new HashSet<>(lastNotifiedPointers);
        removedPointers.removeAll(newPointers);
        removedPointers.forEach(x -> context.getUnlinkListener().unlinked(context.getSelfId(), x.getId()));
        
        lastNotifiedPointers = newPointers;
    }
}
