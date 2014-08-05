package com.offbynull.peernetic.demos.chord.fsms;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.Id;
import com.offbynull.peernetic.common.IncomingRequestManager;
import com.offbynull.peernetic.common.NonceGenerator;
import com.offbynull.peernetic.common.NonceWrapper;
import com.offbynull.peernetic.common.OutgoingRequestManager;
import com.offbynull.peernetic.common.Response;
import com.offbynull.peernetic.demos.chord.core.ExternalPointer;
import static com.offbynull.peernetic.demos.chord.fsms.InitFingerTable.SEND_QUERY_FOR_ID;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.demos.chord.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.fsm.FilterHandler;
import com.offbynull.peernetic.fsm.FiniteStateMachine;
import com.offbynull.peernetic.fsm.StateHandler;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class RouteToFinger<A> {
    public static final String INITIAL_STATE = "start";
    public static final String SEND_STATE = "query";
    public static final String AWAIT_STATE = "await";
    public static final String DONE_STATE = "done";

    private static final Duration TIMER_DURATION = Duration.ofSeconds(3L);

    private final Id findId;
    
    private ExternalPointer<A> currentNode;
    private TaskState taskState = TaskState.RUNNING;

    private final IncomingRequestManager<A, byte[]> incomingRequestManager;
    private final OutgoingRequestManager<A, byte[]> outgoingRequestManager;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;

    public RouteToFinger(ExternalPointer<A> initialNode, Id findId, EndpointDirectory<A> endpointDirectory,
            EndpointScheduler endpointScheduler, Endpoint selfEndpoint, NonceGenerator<byte[]> nonceGenerator,
            NonceWrapper<byte[]> nonceWrapper) {
        Validate.notNull(initialNode);
        Validate.notNull(findId);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(nonceGenerator);
        Validate.notNull(nonceWrapper);
        
        this.findId = findId;
        this.currentNode = initialNode;
        this.incomingRequestManager = new IncomingRequestManager<>(selfEndpoint, nonceWrapper);
        this.outgoingRequestManager = new OutgoingRequestManager<>(selfEndpoint, nonceGenerator, nonceWrapper, endpointDirectory);
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
    }
    
    @StateHandler(INITIAL_STATE)
    public void handleStart(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint) {
        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
        fsm.switchStateAndProcess(SEND_QUERY_FOR_ID, instant, unused, srcEndpoint);
    }

    @FilterHandler({SEND_STATE, AWAIT_STATE})
    public boolean filterResponses(String state, FiniteStateMachine fsm, Instant instant, Response response, Endpoint srcEndpoint)
            throws Exception {
        return outgoingRequestManager.testResponseMessage(instant, response);
    }
    
    @StateHandler(SEND_STATE)
    public void handleSendAskForIdRequest(String state, FiniteStateMachine fsm, Instant instant, Object unused, Endpoint srcEndpoint)
            throws Exception {
        byte[] idData = currentNode.getId().getValueAsByteArray();
        outgoingRequestManager.sendRequestAndTrack(instant, new GetClosestPrecedingFingerRequest(idData), currentNode.getAddress());
        fsm.setState(AWAIT_STATE);
    }

    @StateHandler(AWAIT_STATE)
    public void handleReceiveAskForIdResponse(String state, FiniteStateMachine fsm, Instant instant,
            GetClosestPrecedingFingerResponse<A> response, Endpoint srcEndpoint) throws Exception {
        A address = response.getAddress();
        byte[] idData = response.getId();
        
        Id id = new Id(idData, currentNode.getId().getLimitAsByteArray());
        if (id.equals(currentNode.getId()) && address == null) {
            // node that's returned is currentNode...  we can't go anywhere else so return currentNode as the routed node
            taskState = TaskState.FOUND_EXTERNAL;
            fsm.switchStateAndProcess(DONE_STATE, instant, new Object()/*unused obj*/, srcEndpoint);
        } else if (!id.equals(currentNode.getId()) && address != null) {
            ExternalPointer<A> nextNode = new ExternalPointer<>(id, address);
            currentNode = nextNode;
            if (findId.isWithin(currentNode.getId(), false, nextNode.getId(), true)) {
                // node found, stop here
                taskState = TaskState.FOUND_EXTERNAL;
                fsm.switchStateAndProcess(DONE_STATE, instant, new Object()/*unused obj*/, srcEndpoint);
            } else {
                fsm.switchStateAndProcess(SEND_STATE, instant, new Object()/*unused obj*/, srcEndpoint);
            }
        } else {
            // we have a node id but no address, node gave us bad response so we want to stop here and report an error
            taskState = TaskState.BAD_RESPONSE;
            fsm.switchStateAndProcess(DONE_STATE, instant, new Object()/*unused obj*/, srcEndpoint);
        }
    }
    
    @StateHandler({AWAIT_STATE})
    public void handleTimerTrigger(String state, FiniteStateMachine fsm, Instant instant, InitFingerTable.TimerTrigger response, Endpoint srcEndpoint) {
        incomingRequestManager.process(instant);
        outgoingRequestManager.process(instant);

        endpointScheduler.scheduleMessage(TIMER_DURATION, selfEndpoint, selfEndpoint, new TimerTrigger());
    }

    REMOVE THE FOLLOWING METHODS. IN THERE PLACE, MAKE IT SO THE FSM PARAM (currentl srcEndpoint) CAN AHVE A FIELD THAT IS SET TO INDICATE FINISHED/GIVEBACK RESULT;
    THIS IS UNTESTED;
    public boolean isFinishedRunning() {
        return taskState != TaskState.RUNNING;
    }
    
    public ExternalPointer<A> getResult() {
        if (taskState == TaskState.RUNNING) {
            throw new IllegalStateException("Still running");
        }

        if (taskState == TaskState.BAD_RESPONSE) {
            throw new IllegalStateException("Node responded incorrectly");
        }

        return currentNode;
    }
    
    private enum TaskState {
        RUNNING,
        FOUND_EXTERNAL,
        FOUND_SELF,
        BAD_RESPONSE
    }

    public static final class TimerTrigger {
        private TimerTrigger() {
            // does nothing, prevents outside instantiation
        }
    }
}
