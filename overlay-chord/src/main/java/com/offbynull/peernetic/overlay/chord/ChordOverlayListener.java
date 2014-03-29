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
package com.offbynull.peernetic.overlay.chord;

import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.util.List;

/**
 * Receives notifications when the state of a chord node changes.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public interface ChordOverlayListener<A> {
    /**
     * Chord state has updated.
     * @param event string describing the event that caused the update
     * @param self pointer of the node updated
     * @param predecessor predecessor of {@code self}
     * @param fingerTable finger table of {@code self}
     * @param successorTable successor table of {@code self}
     */
    void stateUpdated(String event,
            Pointer<A> self, Pointer<A> predecessor, List<Pointer<A>> fingerTable, List<Pointer<A>> successorTable);
    /**
     * Chord has failed.
     * @param failureMode failure mode
     */
    void failed(FailureMode failureMode);
    
    /**
     * Describes the failure type.
     */
    public enum FailureMode {
        /**
         * Indicates that the node ran out of successors.
         */
        SUCCESSOR_TABLE_DEPLETED,
        /**
         * Indicates that the node failed to initialize.
         */
        INITIALIZATION_FAILED
    }
}
