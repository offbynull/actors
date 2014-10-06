package com.offbynull.peernetic.playground.chorddht.shared;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.common.javaflow.FlowControl;
import com.offbynull.peernetic.javaflow.TaskState;
import com.offbynull.peernetic.playground.chorddht.ChordContext;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.FindSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetIdResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetPredecessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.GetSuccessorResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.NotifyResponse;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableRequest;
import com.offbynull.peernetic.playground.chorddht.messages.external.UpdateFingerTableResponse;
import com.offbynull.peernetic.playground.chorddht.tasks.InitFingerTableTask;
import com.offbynull.peernetic.playground.chorddht.tasks.RouteToPredecessorTask;
import com.offbynull.peernetic.playground.chorddht.tasks.RouteToTask;
import com.offbynull.peernetic.playground.chorddht.tasks.UpdateOthersTask;
import java.time.Duration;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class ChordHelper<A, N> {
    private final TaskState taskState;
    private final FlowControl<A, N> flowControl;
    private final ChordContext<A> context;

    public ChordHelper(TaskState taskState, FlowControl flowControl, ChordContext context) {
        Validate.notNull(taskState);
        Validate.notNull(flowControl);
        Validate.notNull(context);

        this.taskState = taskState;
        this.flowControl = flowControl;
        this.context = context;
    }
    
    public GetIdResponse sendGetIdRequest(A address) {
        Validate.notNull(address);
        return sendAndWait(new GetIdRequest(), GetIdResponse.class, address);
    }

    public FindSuccessorResponse<A> sendFindSuccessorRequest(A address, Id findId) {
        Validate.notNull(address);
        Validate.notNull(findId);
        return sendAndWait(new FindSuccessorRequest(findId.getValueAsByteArray()), FindSuccessorResponse.class, address);
    }

    public GetPredecessorResponse<A> sendGetPredecessorRequest(A address) {
        Validate.notNull(address);
        return sendAndWait(new GetPredecessorRequest(), GetPredecessorResponse.class, address);
    }

    public GetSuccessorResponse<A> sendGetSuccessorRequest(A address) {
        Validate.notNull(address);
        return sendAndWait(new GetSuccessorRequest(), GetSuccessorResponse.class, address);
    }
    
    public UpdateFingerTableResponse sendUpdateFingerTableRequest(A address, Id id) {
        Validate.notNull(address);
        Validate.notNull(id);
        return sendAndWait(new UpdateFingerTableRequest(id.getValueAsByteArray()), UpdateFingerTableResponse.class, address);
    }

    public GetClosestPrecedingFingerResponse sendGetClosestPrecedingFingerRequest(A address, Id id) {
        Validate.notNull(address);
        Validate.notNull(id);
        return sendAndWait(new GetClosestPrecedingFingerRequest(id.getValueAsByteArray()), GetClosestPrecedingFingerResponse.class, 
                address);
    }

    public GetClosestFingerResponse sendGetClosestFingerRequest(A address, Id id, Id skipId) {
        Validate.notNull(address);
        Validate.notNull(id);
        return sendAndWait(new GetClosestFingerRequest(id.getValueAsByteArray(), skipId.getValueAsByteArray()),
                GetClosestFingerResponse.class, address);
    }

    public NotifyResponse<A> sendNotifyRequest(A address, Id id) {
        Validate.notNull(address);
        Validate.notNull(id);
        return sendAndWait(new NotifyRequest(id.getValueAsByteArray()), NotifyResponse.class, address);
    }

    public void fireUpdateFingerTableRequest(A address, Id id) {
        Validate.notNull(address);
        Validate.notNull(id);
        fire(new UpdateFingerTableRequest(id.getValueAsByteArray()), address);
    }
    
    private <T> T sendAndWait(Object request, Class<T> responseType, A address) {
        Validate.notNull(request);
        Validate.notNull(responseType);
        Validate.notNull(address);
        T ret = flowControl.sendRequestAndWait(request, address, responseType, context.getRequestResendDuration(),
                context.getRequestMaxResends(), context.getTotalTrackRequestDuration());
        
        if (ret == null) {
            throw new ChordOperationException(request, responseType);
        }
        
        return ret;
    }

    private void fire(Object request, A address) {
        Validate.notNull(request);
        Validate.notNull(address);
        flowControl.sendRequest(request, address, context.getRequestResendDuration(),
                context.getRequestMaxResends(), context.getTotalTrackRequestDuration());
    }
    
    
    
    
    
    
    public void trackRequest(Object incomingRequest) {
        context.getRouter().trackRequest(taskState.getTime(), incomingRequest, taskState.getSource(),
                context.getTotalTrackResponeDuration());
    }
    
    public void sendGetIdResponse(GetIdRequest originalRequest, Endpoint originalSource, Id selfId) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        Validate.notNull(selfId);
        
        GetIdResponse response = new GetIdResponse(selfId.getValueAsByteArray());
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }

    public void sendGetPredecessorResponse(GetPredecessorRequest originalRequest, Endpoint originalSource, ExternalPointer<A> finger) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
//        Validate.notNull(finger); // may be null (we may not have a predecessor)
        
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(finger);
        
        GetPredecessorResponse<A> response = new GetPredecessorResponse<>(msgValues.getLeft(), msgValues.getRight());
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }

    public void sendGetSuccessorResponse(GetSuccessorRequest originalRequest, Endpoint originalSource, List<Pointer> successors) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        Validate.noNullElements(successors);
        
        GetSuccessorResponse<A> response = new GetSuccessorResponse<>(successors);
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }

    public void sendGetClosestFingerResponse(GetClosestFingerRequest originalRequest, Endpoint originalSource, Pointer finger) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        Validate.notNull(finger);
        
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(finger);
        
        GetClosestFingerResponse<A> response = new GetClosestFingerResponse<>(msgValues.getLeft(), msgValues.getRight());
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }

    public void sendGetClosestPrecedingFingerResponse(GetClosestPrecedingFingerRequest originalRequest, Endpoint originalSource,
            Pointer finger) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        Validate.notNull(finger);
        
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(finger);
        
        GetClosestPrecedingFingerResponse<A> response = new GetClosestPrecedingFingerResponse<>(msgValues.getLeft(), msgValues.getRight());
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }

    public void sendNotifyResponse(NotifyRequest originalRequest, Endpoint originalSource, ExternalPointer<A> predecessor) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        Validate.notNull(predecessor);
        
        ImmutablePair<byte[], A> msgValues = convertPointerToMessageDetails(predecessor);
        
        NotifyResponse<A> response = new NotifyResponse<>(msgValues.getLeft(), msgValues.getRight());
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }
    
    public void sendFindSuccessorResponse(FindSuccessorRequest originalRequest, Endpoint originalSource, Id foundId,
            A foundAddress) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        Validate.notNull(foundId);
//        Validate.notNull(foundAddress); // may be null (null = self address)
        
        FindSuccessorResponse<A> response = new FindSuccessorResponse<>(foundId.getValueAsByteArray(), foundAddress);
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
    }

    public void sendUpdateFingerTableResponse(UpdateFingerTableRequest originalRequest, Endpoint originalSource) {
        Validate.notNull(originalRequest);
        Validate.notNull(originalSource);
        
        UpdateFingerTableResponse response = new UpdateFingerTableResponse();
        context.getRouter().sendResponse(taskState.getTime(), originalRequest, response, originalSource);
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
    
    
    
    
    
    public Pointer runRouteToTask(Id id) throws Exception {
        RouteToTask<A> task = RouteToTask.create(taskState.getTime(), context, id);
        flowControl.waitUntilFinished(task.getActor(), Duration.ofSeconds(1L));
        Pointer ret = task.getResult();
        
        if (ret == null) {
            throw new ChordOperationException(task.getClass());
        }
        
        return ret;
    }

    public Pointer runRouteToPredecessorTask(Id id) throws Exception {
        RouteToPredecessorTask<A> task = RouteToPredecessorTask.create(taskState.getTime(), context, id);
        flowControl.waitUntilFinished(task.getActor(), Duration.ofSeconds(1L));
        Pointer ret = task.getResult();
        
        if (ret == null) {
            throw new ChordOperationException(task.getClass());
        }
        
        return ret;
    }

    public void runInitFingerTableTask(A bootstrapAddress, Id bootstrapId) throws Exception {
        InitFingerTableTask<A> initFingerTableTask = InitFingerTableTask.create(taskState.getTime(), context,
                new ExternalPointer<>(bootstrapId, bootstrapAddress));
        flowControl.waitUntilFinished(initFingerTableTask.getActor(), Duration.ofSeconds(1L));
    }
    
    public void fireUpdateOthersTask() throws Exception {
        UpdateOthersTask.create(taskState.getTime(), context);
    }
    
    
    
    
    
    

    public void setImmediateSuccessor(Pointer ptr) {
        Validate.notNull(ptr);
        context.getSuccessorTable().updateTrim(ptr);
    }
    
    public void setPredecessor(GetPredecessorResponse<A> resp) {
        Validate.notNull(resp);
        if (resp.getId() != null && !isSelfId(resp.getId())) {
            context.setPredecessor(toExternalPointer(resp));
        }
    }

    public void clearPredecessor() {
        context.setPredecessor(null);
    }

    public ExternalPointer<A> getPredecessor() {
        return context.getPredecessor();
    }
    
    public int getFingerTableLength() {
        return IdUtils.getBitLength(context.getSelfId());
    }

    public Pointer getFinger(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return context.getFingerTable().get(idx);
    }

    public void putFinger(ExternalPointer<A> ptr) {
        Validate.notNull(ptr);
        context.getFingerTable().put(ptr);
    }

    public Id getExpectedFingerId(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return context.getFingerTable().getExpectedId(idx);
    }
    
    public void setTables(FingerTable<A> fingerTable, SuccessorTable<A> successorTable) {
        Validate.notNull(fingerTable);
        Validate.notNull(successorTable);
        
        context.setFingerTable(fingerTable);
        context.setSuccessorTable(successorTable);
    }

    public void failIfSelf(GetPredecessorResponse<A> resp) {
        Validate.notNull(resp);
        if (resp.getId() != null && isSelfId(resp.getId())) {
            throw new ChordOperationException("Id in response is set to this node's id.");
        }
    }

    public void failIfSelf(FindSuccessorResponse<A> resp) {
        Validate.notNull(resp);
        if (isSelfId(resp.getId())) {
            throw new ChordOperationException("Id in response is set to this node's id.");
        }
    }
    
    public boolean isSelfId(byte[] idData) {
        Validate.notNull(idData);
        
        return new Id(idData, context.getSelfId().getLimitAsByteArray()).equals(context.getSelfId());
    }
    
    public Id toId(byte[] idData) {
        Validate.notNull(idData);
        
        return new Id(idData, context.getSelfId().getLimitAsByteArray());
    }
    
    public ExternalPointer<A> toExternalPointer(FindSuccessorResponse<A> resp, A defaultAddress) {
        Validate.notNull(resp);
        Validate.notNull(defaultAddress);
        return toExternalPointer(resp.getId(), resp.getAddress(), defaultAddress);
    }

    public ExternalPointer<A> toExternalPointer(GetPredecessorResponse<A> resp) {
        Validate.notNull(resp);
        return toExternalPointer(resp.getId(), resp.getAddress(), resp.getAddress());
    }
    
    private ExternalPointer<A> toExternalPointer(byte[] idData, A address, A defaultAddress) {
        Validate.notNull(idData);
        Validate.notNull(defaultAddress);
        // address can be null

        if (address != null) {
            return new ExternalPointer<>(toId(idData), address);
        } else {
            return new ExternalPointer<>(toId(idData), defaultAddress);
        }
    }    
    
    
    
    
    
    
    public void sleep(long seconds) {
        Validate.isTrue(seconds > 0L);
        flowControl.wait(Duration.ofSeconds(seconds));
    }
}
