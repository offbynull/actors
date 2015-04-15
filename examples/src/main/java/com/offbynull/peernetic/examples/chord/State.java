package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.examples.chord.externalmessages.FindSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.nodeid.NodeIdUtils;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import com.offbynull.peernetic.examples.common.request.ExternalMessageIdGenerator;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class State {
    
    private final String timerPrefix;
    
    private ExternalMessageIdGenerator externalMessageIdGenerator = new ExternalMessageIdGenerator(new Random());
    
    private FingerTable fingerTable;
    private SuccessorTable successorTable;
    private ExternalPointer predecessor;
    
    private NodeId selfId;
    private String bootstrapAddress;
    
    public State(String timerPrefix) {
        Validate.notNull(timerPrefix);
        this.timerPrefix = timerPrefix;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }

    public NodeId getSelfId() {
        return selfId;
    }

    public String getBootstrapAddress() {
        return bootstrapAddress;
    }

    public void setSelfId(NodeId selfId) {
        this.selfId = selfId;
    }

    public void setBootstrapAddress(String bootstrapAddress) {
        this.bootstrapAddress = bootstrapAddress;
    }

    public FingerTable getFingerTable() {
        return fingerTable;
    }

    public void setFingerTable(FingerTable fingerTable) {
        this.fingerTable = fingerTable;
    }

    public SuccessorTable getSuccessorTable() {
        return successorTable;
    }

    public void setSuccessorTable(SuccessorTable successorTable) {
        this.successorTable = successorTable;
    }

    public ExternalPointer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(ExternalPointer predecessor) {
        this.predecessor = predecessor;
    }

    
    public void setSuccessor(Pointer ptr) {
        Validate.notNull(ptr);
        getSuccessorTable().updateTrim(ptr);
        
        if (isSelfId(ptr.getId())) {
            getFingerTable().clear();
        } else {
            getFingerTable().clearBefore(ptr.getId());
            getFingerTable().put((ExternalPointer) ptr);
        }
    }

    public void moveToNextSuccessor() {
        getSuccessorTable().moveToNextSucessor();
        Pointer ptr = getSuccessorTable().getSuccessor();
        
        if (isSelfId(ptr.getId())) {
            getFingerTable().clear();
        } else {
            getFingerTable().clearBefore(ptr.getId());
            getFingerTable().put((ExternalPointer) ptr);
        }
    }
    
    public void setPredecessor(GetPredecessorResponse resp) {
        Validate.notNull(resp);
        if (resp.getChordId() != null && !isSelfId(resp.getChordId())) {
            setPredecessor(toExternalPointer(resp));
        }
    }

    public void clearPredecessor() {
        setPredecessor((ExternalPointer) null);
    }


    public List<Pointer> getSuccessors() {
        return getSuccessorTable().dump();
    }

    public Pointer getSuccessor() {
        return getSuccessorTable().getSuccessor();
    }
    
    public int getFingerTableLength() {
        return NodeIdUtils.getBitLength(getSelfId());
    }

    public Pointer getFinger(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return getFingerTable().get(idx);
    }

    public Pointer getClosestFinger(GetClosestFingerRequest req) {
        Validate.notNull(req);
        return getFingerTable().findClosest(req.getChordId(), req.getSkipChordId());
    }

    public Pointer getClosestFinger(NodeId id, NodeId ... skipIds) {
        Validate.notNull(id);
        Validate.noNullElements(skipIds);
        return getFingerTable().findClosest(id, skipIds);
    }

    public Pointer getClosestPrecedingFinger(GetClosestPrecedingFingerRequest req) {
        Validate.notNull(req);
        return getFingerTable().findClosestPreceding(req.getChordId());
    }

    public Pointer getClosestPrecedingFinger(NodeId id) {
        Validate.notNull(id);
        return getFingerTable().findClosestPreceding(id);
    }

    public NodeId getExpectedFingerId(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return getFingerTable().getExpectedId(idx);
    }

    public ExternalPointer getMaximumNonSelfFinger() {
        return getFingerTable().getMaximumNonBase();
    }
    
    public NodeId getIdThatShouldHaveThisNodeAsFinger(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return getFingerTable().getRouterId(idx);
    }
    
    public List<Pointer> getFingers() {
        return getFingerTable().dump();
    }
    
    public void putFinger(ExternalPointer ptr) {
        Validate.notNull(ptr);
        getFingerTable().put(ptr);
        
        if (getFingerTable().get(0).equals(ptr)) {
            getSuccessorTable().updateTrim(ptr);
        }
    }
    
    public boolean replaceFinger(ExternalPointer ptr) {
        Validate.notNull(ptr);
        return getFingerTable().replace(ptr);
    }
    
    public void removeFinger(ExternalPointer ptr) {
        Validate.notNull(ptr);
        getFingerTable().remove(ptr);
    }
    
    public void updateSuccessor(ExternalPointer successor, List<Pointer> subsequentSuccessors) {
        Validate.notNull(successor);
        Validate.noNullElements(subsequentSuccessors);
        getSuccessorTable().update(successor, subsequentSuccessors);
        getFingerTable().put(successor);
    }
    
    public void setTables(FingerTable fingerTable, SuccessorTable successorTable) {
        Validate.notNull(fingerTable);
        Validate.notNull(successorTable);
        
        setFingerTable(fingerTable);
        setSuccessorTable(successorTable);
    }

    public InternalPointer getSelfPointer() {
        return new InternalPointer(getSelfId());
    }
    
    public boolean isSelfId(byte[] idData) {
        Validate.notNull(idData);
        return new NodeId(idData, selfId.getLimitAsByteArray()).equals(selfId);
    }

    public boolean isSelfId(NodeId id) {
        Validate.notNull(id);
        return id.equals(selfId);
    }
    
    public void failIfSelf(GetPredecessorResponse resp) {
        Validate.notNull(resp);
        if (resp.getChordId() != null && isSelfId(resp.getChordId())) {
            throw new IllegalArgumentException("Id in response is set to this node's id.");
        }
    }

    public void failIfSelf(FindSuccessorResponse resp) {
        Validate.notNull(resp);
        if (isSelfId(resp.getChordId())) {
            throw new IllegalArgumentException("Id in response is set to this node's id.");
        }
    }
    
    public NodeId toId(byte[] idData) {
        Validate.notNull(idData);
        
        return new NodeId(idData, selfId.getLimitAsByteArray());
    }
    
    public ExternalPointer toExternalPointer(FindSuccessorResponse resp, String defaultAddress) {
        Validate.notNull(resp);
        Validate.notNull(defaultAddress);
        return toExternalPointer(resp.getChordId(), resp.getAddress(), defaultAddress);
    }

    public ExternalPointer toExternalPointer(GetPredecessorResponse resp) {
        Validate.notNull(resp);
        return toExternalPointer(resp.getChordId(), resp.getAddress(), resp.getAddress());
    }
    
    private ExternalPointer toExternalPointer(NodeId idData, String address, String defaultAddress) {
        Validate.notNull(idData);
        Validate.notNull(defaultAddress);
        // address can be null

        if (address != null) {
            return new ExternalPointer(idData, address);
        } else {
            return new ExternalPointer(idData, defaultAddress);
        }
    }

    public long generateExternalMessageId() {
        return externalMessageIdGenerator.generateId();
    }
    
}
