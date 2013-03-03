package com.offbynull.peernetic.chord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class SuccessorTable {
    private Pointer basePtr;
    private ArrayDeque<Pointer> table;
    private int limit;

    public SuccessorTable(Id baseId, Address baseAddress) {
        this(new Pointer(baseId, baseAddress));
    }
    
    public SuccessorTable(Pointer basePtr) {
        if (basePtr == null) {
            throw new NullPointerException();
        }
        
        this.basePtr = basePtr;
        
        Id baseId = basePtr.getId();
        
        limit = baseId.getBitCount();
        
        table = new ArrayDeque<>(limit);
        table.add(basePtr);
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
    
    public void updateTrim(Pointer successor) {
        if (successor == null) {
            throw new NullPointerException();
        }
        
        Id baseId = basePtr.getId();
        Id successorId = successor.getId();
        
        while (!table.isEmpty()) {
            Pointer ptr = table.removeFirst();
            
            Id ptrId = ptr.getId();
            if (ptrId.comparePosition(baseId, successorId) > 0) {
                table.addFirst(ptr);
                break;
            }
        }
        
        table.addFirst(successor);
        
        if (table.size() > limit) {
            table.removeLast();
        }
    }
    
    public void update(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null) {
            throw new NullPointerException();
        }
        
        if (table.size() > limit) {
            table = table.subList(0, limit);
        }

        Id baseId = basePtr.getId();
        int bitCount = baseId.getBitCount();
        
        if (successor.getId().getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        Id lastId = successor.getId();
        int lastTableIdx = -1;
        int idx = 0;
        for (Pointer ptrSuccessor : table) {
            if (ptrSuccessor == null) {
                throw new NullPointerException();
            }
            
            Id ptrSuccessorId = ptrSuccessor.getId();
            
            if (ptrSuccessorId.getBitCount() != bitCount) {
                throw new IllegalArgumentException();
            }
            
            if (ptrSuccessorId.equals(baseId)) {
                lastTableIdx = idx;
                break;
            }
            
            if (ptrSuccessorId.comparePosition(baseId, lastId) < 0) {
                throw new IllegalArgumentException();
            }
            
            idx++;
        }
        
        if (lastTableIdx != -1) {
            table = table.subList(0, lastTableIdx + 1);
        }
        
        int len = table.size() + 1;
        ArrayDeque<Pointer> newTable = new ArrayDeque<>(len);
        
        newTable.add(successor);
        newTable.addAll(table);
        
        this.table = newTable;
    }
    
    public Pointer getSuccessor() {
        return table.peekFirst();
    }
    
    public void moveToNextSucessor() {
        if (table.isEmpty()) {
            throw new IllegalStateException();
        }
        
        table.removeFirst();
    }
    
    public boolean isEmpty() {
        return table.isEmpty();
    }
    
    public boolean isPointingToBase() {
        if (table.isEmpty()) {
            return false;
        }
        
        Pointer ptr = table.getFirst();
        return ptr.equals(basePtr);
    }
    
    public List<Pointer> dump() {
        return new ArrayList<>(table);
    }
}
