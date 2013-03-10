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
        
        adjustFingerTableToMatchPredecessor();
    }
    
    public void removePredecessor() {
        predecessorPtr = null;
    } 

    public Pointer getSuccessor() {
        return successorTable.getSuccessor();
    }
    
    public void shiftSuccessor() {
        successorTable.moveToNextSucessor();

        adjustFingerTableToMatchSuccessorTable();
        adjustFingerTableToMatchPredecessor(); // finger table has changed, make
                                               // sure it doesn't exceed pred
    }

    public void setSuccessor(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null || table.contains(null)) {
            throw new NullPointerException();
        }
        
        successorTable.update(successor, table);

        adjustFingerTableToMatchSuccessorTable();
        adjustFingerTableToMatchPredecessor(); // finger table has changed, make
                                               // sure it doesn't exceed pred
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
        adjustFingerTableToMatchPredecessor(); //incase pred is now < last finger
        adjustSuccessorTableToMatchFingerTable();
    }

    public void removeFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.remove(pointer);
        adjustFingerTableToMatchPredecessor(); //incase pred is now < last finger
        adjustSuccessorTableToMatchFingerTable();
    }
    
    /**
     * Modifies the finger table such that no entry is greater than the
     * predecessor. If the finger table has no entries in it (other than base),
     * the predecessor will be put inside, making at least index 0 (also known
     * as the successor) set to the predecessor.
     * <p/>
     * Since this method potentially modifies index 0, it makes sure that the
     * successor table's first entry matches up with the finger table's
     * successor.
     * <p/>
     * <b>Note</b>: This method does nothing if the predecessor is not set.
     */
    private void adjustFingerTableToMatchPredecessor() {
        if (predecessorPtr == null) {
            return;
        }
        
        Id baseId = basePtr.getId();
        
        Pointer lastFingerPtr = fingerTable.getMaximumNonBase();
        
        if (lastFingerPtr != null) {
            Id lastFingerId = lastFingerPtr.getId();
            Id predecessorId = predecessorPtr.getId();

            if (predecessorId.comparePosition(baseId, lastFingerId) < 0) {
                fingerTable.clearAfter(predecessorId);
                fingerTable.put(predecessorPtr);
            }
        } else {
            fingerTable.put(predecessorPtr);
        }
        
        // ensure successor table in sync with finger table
        adjustSuccessorTableToMatchFingerTable();
    }
    
    private void adjustSuccessorTableToMatchFingerTable() {
        // make sure successor table's value is finger table's idx 0
        Pointer successorPtr = fingerTable.get(0);
        successorTable.updateTrim(successorPtr);
    }
    
    private void adjustFingerTableToMatchSuccessorTable() {
        Pointer successorPtr = successorTable.getSuccessor();

        if (basePtr.equals(successorPtr)) {
            // if the successor is us, clear the finger table such that all
            // fingers = base.
            fingerTable.clear();
        } else {
            // otherwise, get the successor and set it to the finger table
            Id successorId = successorPtr.getId();

                // just to make sure this goes in idx 0, clear everything before
                // the new successor and shove it in
            fingerTable.clearBefore(successorId);
            fingerTable.put(successorPtr);
        }
    }
}
