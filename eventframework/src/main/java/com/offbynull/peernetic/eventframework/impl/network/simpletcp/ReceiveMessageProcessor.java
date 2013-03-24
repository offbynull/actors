package com.offbynull.peernetic.eventframework.impl.network.simpletcp;

import com.offbynull.peernetic.eventframework.event.EventUtils;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.processor.FinishedProcessResult;
import com.offbynull.peernetic.eventframework.processor.OngoingProcessResult;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import com.offbynull.peernetic.eventframework.processor.Processor;
import com.offbynull.peernetic.eventframework.processor.ProcessorException;
import com.offbynull.peernetic.eventframework.processor.ProcessorGroup;
import com.offbynull.peernetic.eventframework.processor.ProcessorGroupResult;
import java.util.LinkedList;
import java.util.List;

public final class ReceiveMessageProcessor implements Processor {
    
    private long serverTrackedId;
    private State state;
    private ReceiveMessageFactory factory;
    private long nextProcGroupId;
    private ProcessorGroup<Long> procGroup;
    private int serverPort;

    public ReceiveMessageProcessor(ReceiveMessageFactory factory,
            int port) {
        if (factory == null) {
            throw new NullPointerException();
        }
        
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException();
        }
        
        this.factory = factory;
        this.serverPort = port;
        this.procGroup = new ProcessorGroup<>(true);
        state = State.INIT;
    }

    public void triggerStop() {
        state = State.STOP_TRIGGER;
    }

    @Override
    public ProcessResult process(long timestamp, IncomingEvent event,
            TrackedIdGenerator trackedIdGen) throws Exception {
        switch (state) {
            case INIT:
                return processInitState(timestamp, event, trackedIdGen);
            case WAIT_SERVER_START:
                return processWaitServerStartState(timestamp, event, trackedIdGen);
            case RECEIVE:
                return processReceiveState(timestamp, event, trackedIdGen);
            case STOP_TRIGGER:
                return processTriggerState(timestamp, event, trackedIdGen);
            case WAIT_SERVER_STOP:
                return processWaitServerStopState(timestamp, event, trackedIdGen);
            case FINISHED:
                return processFinishedState(timestamp, event, trackedIdGen);
            default:
                throw new IllegalStateException();
        }
    }

    private ProcessResult processInitState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        serverTrackedId = trackedIdGen.getNextId();
        OutgoingEvent ssoe = new StartServerOutgoingEvent(serverPort,
                serverTrackedId);
        
        state = State.WAIT_SERVER_START;
        
        return new OngoingProcessResult(ssoe);
    }

    private ProcessResult processWaitServerStartState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        EventUtils.throwProcessorExceptionOnError(event, serverTrackedId,
                ReceiveMessageException.class);
        
        if (EventUtils.isTrackedResponse(event, serverTrackedId)) {
            state = State.RECEIVE;
        }
        
        return new OngoingProcessResult();
    }

    private ProcessResult processReceiveState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen)
            throws Exception {
        EventUtils.throwProcessorExceptionOnError(event, serverTrackedId,
                ReceiveMessageException.class);
        
        ReceiveMessageIncomingEvent rmie = EventUtils.trackedCast(event,
                serverTrackedId);
        
        List<OutgoingEvent> outgoingEvents = new LinkedList<>();
        if (rmie != null) {
            SendResponseOutgoingEvent sroe = factory.createResponse(rmie);
            
            if (sroe != null) {
                outgoingEvents.add(sroe);
            } else {
                Processor proc = factory.createProcessor(rmie);

                if (proc != null) {
                    procGroup.add(nextProcGroupId, proc);
                    nextProcGroupId++;
                }
            }
        }
        
        ProcessorGroupResult<Long> procGroupRes = procGroup.process(timestamp,
                event, trackedIdGen);
        
        outgoingEvents.addAll(procGroupRes.gatherOutgoingEvents());
        
        return new OngoingProcessResult(outgoingEvents);
    }

    private ProcessResult processTriggerState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        StopServerOutgoingEvent ssoe = new StopServerOutgoingEvent(
                serverTrackedId);
        
        state = State.WAIT_SERVER_STOP;
        return new OngoingProcessResult(ssoe);
    }

    private ProcessResult processWaitServerStopState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        EventUtils.throwProcessorExceptionOnError(event, serverTrackedId,
                ReceiveMessageException.class);
        
        if (EventUtils.isTrackedResponse(event, serverTrackedId)) {
            state = State.FINISHED;
        }
        
        return new FinishedProcessResult();
    }

    private ProcessResult processFinishedState(long timestamp,
            IncomingEvent event, TrackedIdGenerator trackedIdGen) {
        throw new IllegalArgumentException();
    }
    
    public static class ReceiveMessageException extends ProcessorException {
        
    }
    
    private enum State {
        INIT,
        WAIT_SERVER_START,
        RECEIVE,
        STOP_TRIGGER,
        WAIT_SERVER_STOP,
        FINISHED
    }
    
    public interface ReceiveMessageFactory {
        SendResponseOutgoingEvent createResponse(
                ReceiveMessageIncomingEvent event);
        Processor createProcessor(ReceiveMessageIncomingEvent event);
    }
}
