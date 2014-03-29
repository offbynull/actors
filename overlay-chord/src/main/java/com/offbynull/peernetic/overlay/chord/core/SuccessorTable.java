/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.overlay.chord.core;

import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.IdUtils;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Holds on to a list of successors. The maximum number of successors this
 * implementation can hold is the bit count in the base pointer's id. The list
 * is assumed to be recursively generated. For more information on how a
 * successor table operates, please refer to the Chord research paper.
 * @param <A> address type
 * @author Kasra Faghihi
 */
public final class SuccessorTable<A> {
    /**
     * Base (self) pointer.
     */
    private Pointer<A> basePtr;
    /**
     * List of successors.
     */
    private ArrayDeque<Pointer<A>> table;
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
    public SuccessorTable(Pointer<A> basePtr) {
        Validate.notNull(basePtr);
        IdUtils.ensureLimitPowerOfTwo(basePtr);
        
        this.basePtr = basePtr;
        limit = IdUtils.getLimitBitLength(basePtr);
        
        table = new ArrayDeque<>(limit);
        table.add(basePtr);
    }
    
    /**
     * Get the base pointer.
     * @return base pointer
     */
    public Pointer<A> getBase() {
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
    public A getBaseAddress() {
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
    public void updateTrim(Pointer<A> successor) {
        Validate.notNull(successor);
        IdUtils.ensureLimitsMatch(basePtr, successor);
        
        Id baseId = basePtr.getId();
        Id successorId = successor.getId();
        
        if (basePtr.equalsEnsureAddress(successor)) {
            table = new ArrayDeque<>();
            table.add(successor);
            return;
        }
        
        while (!table.isEmpty()) {
            Pointer<A> ptr = table.removeFirst();
            
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
     * doesn't exceed. The pointers in {@code table} must be sorted. The point
     * at which {@code table} loops around our base id is the point that it'll
     * be trimmed.
     * @param successor immediate successor
     * @param table successors after {@code successor}
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     * @throws IllegalArgumentException if {@code successor}'s id has a
     * different bit count than the base pointer's id / if the ids of any of the pointers in
     * {@code table} have a different bit count than the base pointer's id
     */
    public void update(Pointer<A> successor, List<Pointer<A>> table) {
        Validate.notNull(successor);
        Validate.noNullElements(table);
        IdUtils.ensureLimitsMatch(basePtr, successor);
        
        if (table.size() > limit) {
            table = table.subList(0, limit);
        }

        Id baseId = basePtr.getId();
        
        Id lastId = successor.getId();
        int lastTableIdx = -1;
        int idx = 0;
        for (Pointer<A> ptrSuccessor : table) {
            IdUtils.ensureLimitsMatch(basePtr, ptrSuccessor);
            
            Id ptrSuccessorId = ptrSuccessor.getId();
            
            if (basePtr.equalsEnsureAddress(ptrSuccessor)) {
                lastTableIdx = idx;
                break;
            }
            
            if (ptrSuccessorId.comparePosition(baseId, lastId) <= 0) {
                lastTableIdx = idx;
                break;
            }
            
            lastId = ptrSuccessor.getId();
            
            idx++;
        }
        
        if (lastTableIdx != -1) {
            table = table.subList(0, lastTableIdx);
        }
        
        int len = table.size() + 1;
        ArrayDeque<Pointer<A>> newTable = new ArrayDeque<>(len);
        
        newTable.add(successor);
        newTable.addAll(table);
        
        this.table = newTable;
    }
    
    /**
     * Gets the immediate successor.
     * @return immediate successor
     */
    public Pointer<A> getSuccessor() {
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
        Pointer<A> ptr = table.getFirst();
        return ptr.equals(basePtr);
    }
    
    /**
     * Dumps out the successor table.
     * @return list of successor table
     */
    public List<Pointer<A>> dump() {
        return new ArrayList<>(table);
    }
}
