package com.offbynull.peernetic.chord;

import java.util.List;

public final class ChordState {
    private Pointer basePtr;
    private FingerTable fingerTable;
    private SuccessorTable successorTable;
    private Pointer predecessor;

    public ChordState(Pointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
        this.basePtr = basePtr;
        fingerTable = new FingerTable(basePtr);
        successorTable = new SuccessorTable(basePtr);
        predecessor = null;
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
        return predecessor;
    }

    public void setPredecessor(Pointer predecessor) {
        if (predecessor == null) {
            throw new NullPointerException();
        }
        
        Id id = basePtr.getId();
        
        if (this.predecessor == null) {
            this.predecessor = predecessor;
        } else {
            Id oldId = this.predecessor.getId();
            Id newId = predecessor.getId();

            if (!newId.isWithin(oldId, false, id, false)) {
                throw new IllegalArgumentException();
            }
        }
        
        // make finger table consistent... clearAfter ensures vals in finger
        // table don't exceed predecessor
        fingerTable.clearAfter(predecessor.getId());
        fingerTable.put(predecessor, true);
    }
    
    public void removePredecessor() {
        predecessor = null;
    } 

    public Pointer getSuccessor() {
        return fingerTable.get(0);
    }
    
    public void shiftSuccessor() {
        successorTable.moveToNextSucessor();
        Pointer successorPtr = successorTable.getSuccessor();
        
        fingerTable.clearBefore(successorPtr.getId());
    }

    public void setSuccessor(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null || table.contains(null)) {
            throw new NullPointerException();
        }
        
        successorTable.update(successor, table);
        
        // make fingertable consistent... clearBefore ensures value will be set
        // as successor
        fingerTable.clearBefore(successor.getId());
        fingerTable.put(successor);
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
        
        Pointer oldSuccessor = fingerTable.get(0);
        fingerTable.put(pointer);
        Pointer newSuccessor = fingerTable.get(0);
        
        // update succesor in successor table if updated in finger table
        if (!oldSuccessor.equals(newSuccessor)) {
            successorTable.updateTrim(pointer);
        }
        
        // update predecessor if last non-base finger entry exceeds predecessor
        Pointer maxFinger = fingerTable.getMaximumNonBase(); // put above
                                                             // ensures this is
                                                             // never null
        if (predecessor == null) {
            predecessor = maxFinger;
        } else {
            Id predecessorId = predecessor.getId();
            Id maxFingerId = maxFinger.getId();
            Id baseId = basePtr.getId();
            
            if (maxFingerId.comparePosition(baseId, predecessorId) > 0) {
                predecessor = maxFinger;
            }
        }
    }

    public void removeFinger(Pointer pointer) {
        if (pointer == null) {
            throw new NullPointerException();
        }
        
        fingerTable.remove(pointer);
    }

    public boolean isDead() {
        Id id = basePtr.getId();
        
        for (Pointer pointer : fingerTable.dump()) {
            if (!pointer.getId().equals(id)) {
                return false;
            }
        }
        
        return true;
    }
}
