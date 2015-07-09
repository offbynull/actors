package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.FingerTable;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.chord.model.SuccessorTable;
import com.offbynull.peernetic.examples.common.AddressTransformer;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class State {
    
    private final Random random;
    private int counter = 0;
    
    private FingerTable fingerTable;
    private SuccessorTable successorTable;
    private ExternalPointer predecessor;
    
    private NodeId selfId;
    
    private final AddressTransformer addressTransformer;
    
    public State(long seed, NodeId selfId, AddressTransformer addressTransformer) {
        Validate.notNull(selfId);
        Validate.notNull(addressTransformer);
        random = new Random(seed);
        this.selfId = selfId;
        this.addressTransformer = addressTransformer;
    }

    public String nextRandomId() {
        long ret = ((long) random.nextInt()) << 32L | (long) counter;
        counter++;
        return "" + ret;
    }
    
    public NodeId getSelfId() {
        return selfId;
    }

    public ExternalPointer getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(ExternalPointer predecessor) {
        this.predecessor = predecessor;
    }

    public void moveToNextSuccessor() {
        successorTable.moveToNextSucessor();
        Pointer ptr = successorTable.getSuccessor();
        
        if (isSelfId(ptr.getId())) {
            fingerTable.clear();
        } else {
            fingerTable.clearBefore(ptr.getId());
            fingerTable.put((ExternalPointer) ptr);
        }
    }
    
    public void clearPredecessor() {
        setPredecessor((ExternalPointer) null);
    }

    public List<Pointer> getSuccessors() {
        return successorTable.dump();
    }

    public Pointer getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    public int getFingerTableLength() {
        return getSelfId().getBitLength();
    }

    public Pointer getFinger(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return fingerTable.get(idx);
    }

    public Pointer getClosestPrecedingFinger(NodeId id, NodeId ... ignoreIds) {
        Validate.notNull(id);
        return fingerTable.findClosestPreceding(id, ignoreIds);
    }

    public NodeId getExpectedFingerId(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return fingerTable.getExpectedId(idx);
    }
    
    public NodeId getIdThatShouldHaveThisNodeAsFinger(int idx) {
        Validate.isTrue(idx >= 0); // method below also checks < tableLength
        return fingerTable.getRouterId(idx);
    }
    
    public List<Pointer> getFingers() {
        return fingerTable.dump();
    }
    
    public void putFinger(ExternalPointer ptr) {
        Validate.notNull(ptr);
        fingerTable.put(ptr);
        
        if (fingerTable.get(0).equals(ptr)) {
            successorTable.updateTrim(ptr);
        }
    }
    
    public boolean replaceFinger(ExternalPointer ptr) {
        Validate.notNull(ptr);
        boolean replaced = fingerTable.replace(ptr);
        
        if (replaced && fingerTable.get(0).equals(ptr)) {
            successorTable.updateTrim(ptr);
        }
        
        return replaced;
    }
    
    public void removeFinger(ExternalPointer ptr) {
        Validate.notNull(ptr);
        if (fingerTable.get(0).equals(ptr)) {
            successorTable.moveToNextSucessor();
            fingerTable.remove(ptr);
            if (!successorTable.isPointingToBase()) {
                fingerTable.put((ExternalPointer) successorTable.getSuccessor());
            }
        } else {
            fingerTable.remove(ptr);
        }
    }
    
    public void updateSuccessor(ExternalPointer successor, List<Pointer> subsequentSuccessors) {
        Validate.notNull(successor);
        Validate.noNullElements(subsequentSuccessors);
        successorTable.update(successor, subsequentSuccessors);
        fingerTable.put(successor);
    }
    
    public void setTables(FingerTable fingerTable, SuccessorTable successorTable) {
        Validate.notNull(fingerTable);
        Validate.notNull(successorTable);
        
        this.fingerTable = fingerTable; // should really be copying these in
        this.successorTable = successorTable; // should really be copying these in
    }

    public InternalPointer getSelfPointer() {
        return new InternalPointer(getSelfId());
    }

    public boolean isSelfId(NodeId id) {
        Validate.notNull(id);
        return id.equals(selfId);
    }
    
    public ExternalPointer toExternalPointer(NodeId idData, String linkId, String defaultLinkId) {
        Validate.notNull(idData);
        Validate.notNull(defaultLinkId);
        // address can be null

        if (linkId != null) {
            return new ExternalPointer(idData, linkId);
        } else {
            return new ExternalPointer(idData, defaultLinkId);
        }
    }
    
    public Pointer toPointer(NodeId idData, String linkId) {
        Validate.notNull(idData);
        Validate.isTrue(idData.getLimitAsBigInteger().equals(selfId.getLimitAsBigInteger()));
        // address can be null

        Pointer ret;
        if (linkId == null) {
            ret = new InternalPointer(idData);
        } else {
            Validate.isTrue(!idData.equals(selfId));
            ret = new ExternalPointer(idData, linkId);
        }
        
        return ret;
    }

    public ExternalPointer toExternalPointer(NodeId idData, String linkId) {
        Validate.notNull(idData);
        Pointer ret = toPointer(idData, linkId);
        Validate.isTrue(ret instanceof ExternalPointer);
        
        return (ExternalPointer) ret;
    }
    
    public void validateExternalId(Pointer ptr) {
        Validate.notNull(ptr);
        Validate.isTrue(!isSelfId(ptr.getId()));
    }
    
    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }
}
