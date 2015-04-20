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
package com.offbynull.peernetic.examples.chord.externalmessages;

import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.io.Serializable;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class GetClosestPrecedingFingerRequest extends ExternalMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private NodeId chordId;
    private NodeId[] ignoreIds;

    public GetClosestPrecedingFingerRequest(long id, NodeId chordId, NodeId ... ignoreIds) {
        super(id);
        Validate.notNull(chordId);
        Validate.noNullElements(ignoreIds);
        this.chordId = chordId;
        this.ignoreIds = Arrays.copyOf(ignoreIds, ignoreIds.length);
    }

    public NodeId getChordId() {
        return chordId;
    }

    public NodeId[] getIgnoreIds() {
        return Arrays.copyOf(ignoreIds, ignoreIds.length);
    }
    
}
