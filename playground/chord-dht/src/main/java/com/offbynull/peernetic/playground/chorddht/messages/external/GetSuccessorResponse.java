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
package com.offbynull.peernetic.playground.chorddht.messages.external;

import com.offbynull.peernetic.common.message.Response;
import com.offbynull.peernetic.playground.chorddht.model.ExternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.InternalPointer;
import com.offbynull.peernetic.playground.chorddht.model.Pointer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class GetSuccessorResponse<A> extends Response {

    private UnmodifiableList<SuccessorEntry<A>> entries;

    public GetSuccessorResponse(List<Pointer> pointers) {
        Validate.noNullElements(pointers);

        List<SuccessorEntry<A>> entries = new ArrayList<>(pointers.size());
        pointers.stream().map(x -> {
            if (x instanceof InternalPointer) {
                return new InternalSuccessorEntry<A>(x.getId().getValueAsByteArray());
            } else if (x instanceof ExternalPointer) {
                return new ExternalSuccessorEntry<A>(x.getId().getValueAsByteArray(), ((ExternalPointer<A>) x).getAddress());
            } else {
                throw new IllegalArgumentException();
            }
        }).forEachOrdered(x -> entries.add(x));

        this.entries = new UnmodifiableList<>(entries);
        validate();
    }

    public UnmodifiableList<SuccessorEntry<A>> getEntries() {
        return entries;
    }

    @Override
    protected void innerValidate() {
        Validate.noNullElements(entries);
        Validate.isTrue(!entries.isEmpty());
    }

    public static abstract class SuccessorEntry<A> {

        private byte[] id;

        protected SuccessorEntry(byte[] id) {
            Validate.notNull(id);
            this.id = Arrays.copyOf(id, id.length);
        }

        public byte[] getId() {
            return Arrays.copyOf(id, id.length);
        }

    }

    public static final class InternalSuccessorEntry<A> extends SuccessorEntry<A> {

        public InternalSuccessorEntry(byte[] id) {
            super(id);
        }
    }

    public static final class ExternalSuccessorEntry<A> extends SuccessorEntry<A> {

        private A address;

        public ExternalSuccessorEntry(byte[] id, A address) {
            super(id);
            Validate.notNull(address);
            this.address = address;
        }

        public A getAddress() {
            return address;
        }

    }
}
