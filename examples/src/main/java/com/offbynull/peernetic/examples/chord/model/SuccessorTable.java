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
package com.offbynull.peernetic.examples.chord.model;

import com.offbynull.peernetic.examples.common.nodeid.NodeIdUtils;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Holds on to a list of successors. The maximum number of successors this implementation can hold is the bit size in the base pointer's id
 * limit. The list  is assumed to be recursively generated. For more information on how a  successor table operates, please refer to the
 * Chord research paper.
 * @author Kasra Faghihi
 */
public final class SuccessorTable {
    /**
     * Base (self) pointer.
     */
    private InternalPointer basePtr;
    /**
     * List of successors.
     */
    private ArrayDeque<Pointer> table;
    /**
     * Maximum number of successors that can be held (equal to bit size of basePtr's id limit).
     */
    private int limit;
    
    /**
     * Constructs a {@link SuccessorTable} object.
     * @param basePtr base pointer
     * @throws NullPointerException if any arguments are {@code null}
     */
    public SuccessorTable(InternalPointer basePtr) {
        Validate.notNull(basePtr);
        NodeId baseId = basePtr.getId();
        Validate.isTrue(NodeIdUtils.isUseableId(baseId)); // make sure satisfies 2^n-1
        
        this.basePtr = basePtr;
        limit = NodeIdUtils.getBitLength(baseId);
        
        table = new ArrayDeque<>(limit);
        table.add(basePtr);
    }
    
    /**
     * Trims all successors below {@code successor}, then adds {@code successor} to the beginning of the successor list. If
     * {@code successor} is the base pointer, all other successors will be cleared out.
     * @param successor new immediate successor
     * @throws NullPointerException if any arguments are {@code null}
     * @throws IllegalArgumentException if {@code successor}'s id has a different limit bit size than the base pointer's id
     */
    public void updateTrim(Pointer successor) {
        Validate.notNull(successor);
        
        NodeId baseId = basePtr.getId();
        NodeId successorId = successor.getId();
        
        Validate.isTrue(NodeIdUtils.getBitLength(successorId) == limit);
        
        if (baseId.equals(successorId)) { // test above makes sure if this is true, successor will be of correct type
            table = new ArrayDeque<>();
            table.add(successor);
            return;
        }
        
        while (!table.isEmpty()) {
            Pointer ptr = table.removeFirst();
            
            NodeId ptrId = ptr.getId();
            if (NodeId.comparePosition(baseId, ptrId, successorId) > 0) {
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
     * Updates this successor table so that the immediate successor is {@code successor} and the successors after it are {@code table}. If
     * {@code table} contains the base pointer in it, all pointers including and after the base pointer won't be added. If {@code table}'s
     * size exceeds the number of bits in base pointer's id, it'll be trimmed so that it doesn't exceed. The pointers in {@code table} must
     * be sorted. The point at which {@code table} loops around our base id is the point that it'll be trimmed.
     * @param successor immediate successor
     * @param table successors after {@code successor}
     * @throws NullPointerException if any arguments are {@code null} or contain {@code null}
     * @throws IllegalArgumentException if {@code successor}'s id has a different limit bit size than the base pointer's id, or if the ids
     * of any of the pointers in {@code table} have a different limit bit size than the base pointer's id
     */
    public void update(Pointer successor, List<Pointer> table) {
        Validate.notNull(successor);
        Validate.noNullElements(table);
        
        NodeId baseId = basePtr.getId();
        NodeId successorId = successor.getId();
        
        Validate.isTrue(NodeIdUtils.getBitLength(successorId) == limit);
        
        if (table.size() > limit) {
            table = table.subList(0, limit);
        }
        
        NodeId lastId = successor.getId();
        int lastTableIdx = -1;
        int idx = 0;
        for (Pointer ptrSuccessor : table) {
            NodeId ptrSuccessorId = ptrSuccessor.getId();
            
            Validate.isTrue(NodeIdUtils.getBitLength(ptrSuccessorId) == limit);
            
            if (baseId.equals(ptrSuccessorId)) {
                lastTableIdx = idx;
                break;
            }
            
            if (NodeId.comparePosition(baseId, ptrSuccessorId, lastId) <= 0) {
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
        ArrayDeque<Pointer> newTable = new ArrayDeque<>(len);
        
        newTable.add(successor);
        table.forEach(x -> {
            NodeId id = x.getId();
            if (id.equals(baseId)) {
                newTable.add(new InternalPointer(id)); // if referencing self, use internalpointer
            } else {
                newTable.add(x);
            }
        });
        
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
        Validate.isTrue(table.size() > 1); // if you only have 1 left and you remove it, you have no successor (which is not allowed)
        table.removeFirst();
    }
    
    /**
     * Checks to see if the immediate successor is pointing to base.
     * @return {@code true} if the immediate successor is base, {@code false}
     * otherwise
     */
    public boolean isPointingToBase() {
        Pointer ptr = table.getFirst();
        return ptr instanceof InternalPointer;
    }
    
    /**
     * Dumps out the successor table.
     * @return list of successor table
     */
    public List<Pointer> dump() {
        return new ArrayList<>(table);
    }
}
