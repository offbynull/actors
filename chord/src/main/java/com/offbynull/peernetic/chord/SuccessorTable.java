package com.offbynull.peernetic.chord;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds on to a list of successors. The maximum number of successors this
 * implementation can hold is the bit count in the base pointer's id. The list
 * is assumed to be recursively generated. For more information on how a
 * successor table operates, please refer to the Chord research paper.
 * @author Kasra Faghihi
 */
public final class SuccessorTable {
    /**
     * Base (self) pointer.
     */
    private Pointer basePtr;
    /**
     * List of successors.
     */
    private ArrayDeque<Pointer> table;
    /**
     * Maximum number of successors that can be held (maps to basePtr's id bit
     * count).
     */
    private int limit;
    
    /**
     * Constructs a {@link SuccessorTable} object.
     * @param basePtr base pointer
     * @throws NullPointerException if any arguments are {@code null}
     */
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
    
    /**
     * Get the base pointer.
     * @return base pointer
     */
    public Pointer getBase() {
        return basePtr;
    }
    
    /**
     * Get the id from the base pointer. Equivalent to calling
     * {@code getBase().getId()}.
     * @return base pointer id
     */
    public Id getBaseId() {
        return basePtr.getId();
    }
    
    /**
     * Get the address from the base pointer. Equivalent to calling
     * {@code getBase().getAddress()}.
     * @return base pointer address
     */
    public Address getBaseAddress() {
        return basePtr.getAddress();
    }
    
    /**
     * Trims all successors below {@code successor}, then adds {@code successor}
     * to the beginning of the successor list. If {@code successor} is the base
     * pointer, all other successors will be cleared out.
     * @param successor new immediate successor
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code successor}'s id has a
     * different bit count than the base pointer's id
     */
    public void updateTrim(Pointer successor) {
        if (successor == null) {
            throw new NullPointerException();
        }
        
        Id baseId = basePtr.getId();
        int bitCount = baseId.getBitCount();
        
        Id successorId = successor.getId();
        
        if (successorId.getBitCount() != bitCount) {
            throw new IllegalArgumentException();
        }
        
        if (basePtr.equalsEnsureAddress(successor)) {
            table = new ArrayDeque<>();
            table.add(successor);
            return;
        }
        
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
    
    /**
     * Updates this successor table so that the immediate successor is
     * {@code successor} and the successors after it are {@code table}. If
     * {@code table} contains the base pointer in it, all pointers including and
     * after the base pointer won't be added. If {@code table}'s size exceeds
     * the number of bits in base pointer's id, it'll be trimmed so that it
     * doesn't exceed. The pointers in {@code table} must be sorted so that an
     * entry doesn't exceed the next entry.
     * @param successor immediate successor
     * @param table successors after {@code successor}
     * @throws NullPointerException if any arguments are {@code null}
     * @throws NullPointerException if any of the elements in {@code table}
     * are {@code null}
     * @throws IllegalArgumentException if {@code successor}'s id has a
     * different bit count than the base pointer's id
     * @throws IllegalArgumentException if the ids of any of the pointers in
     * {@code table} have a different bit count than the base pointer's id
     * @throws IllegalArgumentException if {@code table} isn't sorted
     */
    public void update(Pointer successor, List<Pointer> table) {
        if (successor == null || table == null) {
            throw new NullPointerException();
        }
        
        if (table.size() > limit) {
            table = table.subList(0, limit);
        }

        Id baseId = basePtr.getId();
        int bitCount = baseId.getBitCount();
        
        Id successorId = successor.getId();
        
        if (successorId.getBitCount() != bitCount) {
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
            
            if (basePtr.equalsEnsureAddress(ptrSuccessor)) {
                lastTableIdx = idx;
                break;
            }
            
            if (ptrSuccessorId.comparePosition(baseId, lastId) <= 0) {
                throw new IllegalArgumentException();
            }
            
            lastId = ptrSuccessor.getId();
            
            idx++;
        }
        
        if (lastTableIdx != -1) {
            table = table.subList(0, lastTableIdx);
        }
        
        int len = table.size() + 1;
        ArrayDeque<Pointer> newTable = new ArrayDeque<>(len);
        
        newTable.add(successor);
        newTable.addAll(table);
        
        this.table = newTable;
    }
    
    /**
     * Gets the immediate successor.
     * @return immediate successor
     */
    public Pointer getSuccessor() {
        return table.peekFirst();
    }
    
    /**
     * Moves to the next successor.
     * @throws IllegalStateException if there's only one successor in the
     * successor table
     */
    public void moveToNextSucessor() {
        if (table.size() == 1) {
            throw new IllegalStateException();
        }
        
        table.removeFirst();
    }
    
    /**
     * Checks to see if the immediate successor is pointing to base.
     * @return {@code true} if the immediate successor is base, {@code false}
     * otherwise
     */
    public boolean isPointingToBase() {
        Pointer ptr = table.getFirst();
        return ptr.equals(basePtr);
    }
    
    /**
     * Dumps out the successor table.
     * @return list of successor table
     */
    public List<Pointer> dump() {
        return new ArrayList<>(table);
    }
}
