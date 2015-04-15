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

import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.InternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class GetSuccessorResponse extends ExternalMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private UnmodifiableList<SuccessorEntry> entries;

    public GetSuccessorResponse(long id, List<Pointer> pointers) {
        super(id);
        
        Validate.noNullElements(pointers);

        List<SuccessorEntry> entries = new ArrayList<>(pointers.size());
        pointers.stream().map(x -> {
            if (x instanceof InternalPointer) {
                return new InternalSuccessorEntry(x.getId());
            } else if (x instanceof ExternalPointer) {
                return new ExternalSuccessorEntry(x.getId(), ((ExternalPointer) x).getAddress());
            } else {
                throw new IllegalArgumentException();
            }
        }).forEachOrdered(x -> entries.add(x));

        this.entries = new UnmodifiableList<>(entries);
//        validate();
    }

    public UnmodifiableList<SuccessorEntry> getEntries() {
        return entries;
    }

//    @Override
//    protected void innerValidate() {
//        Validate.noNullElements(entries);
//        Validate.isTrue(!entries.isEmpty());
//    }

    public static abstract class SuccessorEntry {

        private NodeId chordId;

        protected SuccessorEntry(NodeId chordId) {
            Validate.notNull(chordId);
            this.chordId = chordId;
        }

        public NodeId getChordId() {
            return chordId;
        }

    }

    public static final class InternalSuccessorEntry extends SuccessorEntry {

        public InternalSuccessorEntry(NodeId chordId) {
            super(chordId);
        }
    }

    public static final class ExternalSuccessorEntry extends SuccessorEntry {

        private String address;

        public ExternalSuccessorEntry(NodeId chordId, String address) {
            super(chordId);
            Validate.notNull(address);
            this.address = address;
        }

        public String getAddress() {
            return address;
        }

    }
}
