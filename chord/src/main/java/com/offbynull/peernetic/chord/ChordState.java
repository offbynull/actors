package com.offbynull.peernetic.chord;

import java.util.List;

public final class ChordState {
    private Pointer basePtr;
    private FingerTable fingerTable;
    private SuccessorTable successorTable;
    private Pointer predeccesor;

    public ChordState(Pointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
        this.basePtr = basePtr;
        fingerTable = new FingerTable(basePtr);
        successorTable = new SuccessorTable(basePtr);
        predeccesor = null;
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

    public Pointer getPredeccesor() {
        return predeccesor;
    }

    public void setPredeccesor(Pointer predeccesor) {
        if (predeccesor == null) {
            throw new NullPointerException();
        }
        
        Id id = basePtr.getId();
        
        if (this.predeccesor == null) {
            this.predeccesor = predeccesor;
        } else {
            Id oldId = this.predeccesor.getId();
            Id newId = predeccesor.getId();

            if (!newId.isWithin(oldId, false, id, false)) {
                throw new IllegalArgumentException();
            }
        }
        
        // make finger table consistent... clearAfter ensures vals in
        // finger table don't exceed predecessor
        fingerTable.clearAfter(predeccesor.getId());
        fingerTable.put(predeccesor); ONLY SET THIS IF VALUES WERE CLEARED
    }
    
    public void removePredecessor() {
        predeccesor = null;
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
        
        WE NEED TO CHECK THAT THIS DOESNT OVERSHOOT THE PREDECESSOR OR
                OVERWRITE THE SUCCESSOR. FINGERTABLE[0] NEEDS TO BE INSYNC WITH
                        SUCCESSORTABLE;
                
                IF > PREDECESSOR. UPDATE PREDECESSOR.;
                IF PLACED IN INDEX 0. UPDATE SUCCESSORTABLE;
        fingerTable.put(pointer);
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
