package com.offbynull.peernetic.chord;

import java.util.List;

public final class ChordState {
    private Pointer basePtr;
    private FingerTable fingerTable;
    private SuccessorTable successorTable;
    private Pointer predecessorPtr;

    public ChordState(Pointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
        this.basePtr = basePtr;
        fingerTable = new FingerTable(basePtr);
        successorTable = new SuccessorTable(basePtr);
        predecessorPtr = null;
    }

    public int getBitCount() {
        return basePtr.getId().getBitCount();
    }
    
    public Pointer getBase() {
        return basePtr;
    }
    
    public Id getBaseId() {
        return basePtr.getId();
    }
    
    public Address getBaseAddress() {
        return basePtr.getAddress();
    }

    public Pointer getPredecessor() {
        return predecessorPtr;
    }

    public void setPredecessor(Pointer predecessor) {
        if (predecessor == null) {
            throw new NullPointerException();
        }
        
        Id id = basePtr.getId();
        
        if (this.predecessorPtr == null) {
            this.predecessorPtr = predecessor;
        } else {
            Id oldId = this.predecessorPtr.getId();
            Id newId = predecessor.getId();

            if (!newId.isWithin(oldId, false, id, false)) {
                throw new IllegalArgumentException();
            }
        }
        
        syncSuccessorTableToFingerTable();
    }
    
    public void removePredecessor() {
        predecessorPtr = null;
        
        syncSuccessorTableToFingerTable();
    } 

    public Pointer getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    public void shiftSuccessor() {
        successorTable.moveToNextSucessor();
        
        if (successorTable.isEmpty()) {
            return;
        }

        syncSuccessorTableToFingerTable();
        syncFingerTableToPredecessor(); // incase successor is only entry in
                                        // finger table and predecessor is now
                                        // greater than successor
    }

    public void setSuccessor(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null || table.contains(null)) {
            throw new NullPointerException();
        }
        
        successorTable.update(successor, table);
        syncSuccessorTableToFingerTable();
        syncFingerTableToPredecessor(); // incase successor is only entry in
                                        // finger table and predecessor is now
                                        // greater than successor
    }

    private void syncFingerTableToPredecessor() {
        // If predecessor is < last non-self finger id, update predecessor to be
        // last non-self finger id
        Pointer lastFingerPtr = fingerTable.getMaximumNonBase();
        if (lastFingerPtr == null) {
            // Nothing exists in the finger table, so trash the predecessor
            predecessorPtr = null;
        } else if (predecessorPtr == null) {
            // There is no predecessor, so set the last finger as the
            // predecessor
            predecessorPtr = lastFingerPtr;
        } else {
            // There is a predecessor, so make sure it's < last finger. If it
            // isn't, then set predecessor to last finger because it doesn't
            // sense for there to be a node after the node that's suppose to
            // be our predecessor (that isn't us).
            Id lastFingerId = lastFingerPtr.getId();
            Id predecessorId = predecessorPtr.getId();
            Id baseId = basePtr.getId();
            
            if (lastFingerId.comparePosition(baseId, predecessorId) > 0) {
                predecessorPtr = lastFingerPtr;
            }
        }
    }
    
    private void syncSuccessorTableToFingerTable() {
        // Trust in the successor table... adjust finger table so that anything
        // before the new successor gets removed and the new successor is set as
        // fingerTable[0]. However, if the successor is us, clear the finger
        // table such that all fingers = base.
        Pointer successorPtr = successorTable.getSuccessor();

        if (!basePtr.equals(successorPtr)) {
            Id successorId = successorPtr.getId();

            fingerTable.clearBefore(successorId);
            fingerTable.put(successorPtr);
        } else {
            fingerTable.clear();
        }
    }
    
    public RouteResult route(Id id) {
        if (id == null) {
            throw new NullPointerException();
        }
        
        return fingerTable.route(id);
    }
    
    public Id getExpectedFingerId(int bitPosition) {
        if (bitPosition < 0) {
            throw new IllegalArgumentException();
        }
        
        return fingerTable.getExpectedId(bitPosition);
    }

    public Pointer getFinger(int bitPosition) {
        if (bitPosition < 0) {
            throw new IllegalArgumentException();
        }
        
        return fingerTable.get(bitPosition);
    }
    
    public void putFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.put(pointer);
        syncFingerTableToSuccessorTableAndPredecessor();
    }

    public void removeFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.remove(pointer);
        syncFingerTableToSuccessorTableAndPredecessor();
    }
    
    private void syncFingerTableToSuccessorTableAndPredecessor() {
        // Force successorTable to use the value from fingerTable[0]
        Pointer successorPtr = fingerTable.get(0);
        successorTable.updateTrim(successorPtr);
        
        // If predecessor is < last non-self finger id, update predecessor to be
        // last non-self finger id
        Pointer lastFingerPtr = fingerTable.getMaximumNonBase();
        if (lastFingerPtr == null) {
            // Nothing exists in the finger table, so trash the predecessor
            predecessorPtr = null;
        } else if (predecessorPtr == null) {
            // There is no predecessor, so set the last finger as the
            // predecessor
            predecessorPtr = lastFingerPtr;
        } else {
            // There is a predecessor, so make sure it's < last finger. If it
            // isn't, then set predecessor to last finger because it doesn't
            // sense for there to be a node after the node that's suppose to
            // be our predecessor (that isn't us).
            Id lastFingerId = lastFingerPtr.getId();
            Id predecessorId = predecessorPtr.getId();
            Id baseId = basePtr.getId();
            
            if (lastFingerId.comparePosition(baseId, predecessorId) > 0) {
                predecessorPtr = lastFingerPtr;
            }
        }
    }
}
