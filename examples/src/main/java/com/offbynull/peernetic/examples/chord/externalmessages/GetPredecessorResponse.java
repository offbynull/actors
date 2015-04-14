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

import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.util.Arrays;

public final class GetPredecessorResponse extends ExternalMessage {
    private byte[] chordId;
    private String address;

    public GetPredecessorResponse(long id, byte[] chordId, String address) {
        super(id);
        this.chordId = chordId != null ? Arrays.copyOf(chordId, chordId.length) : null;
        this.address = address;
//        validate();
    }

    public byte[] getChordId() {
        return chordId != null ? Arrays.copyOf(chordId, chordId.length) : null;
    }

    public String getAddress() {
        return address;
    }
    
//    @Override
//    protected void innerValidate() {
//        if (chordId != null) {
//            Validate.isTrue(chordId.length > 0);
//        }
//    }
}
