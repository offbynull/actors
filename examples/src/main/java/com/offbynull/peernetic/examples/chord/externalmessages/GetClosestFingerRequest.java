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

public final class GetClosestFingerRequest extends ExternalMessage {
    private byte[] chordId;
    private byte[] skipChordId; //get closest that isn't skipChordId

    public GetClosestFingerRequest(long id, byte[] chordId, byte[] skipChordId) {
        super(id);
        this.chordId = Arrays.copyOf(chordId, chordId.length);
        this.skipChordId = Arrays.copyOf(skipChordId, skipChordId.length);
//        validate();
    }

    public byte[] getChordId() {
        return Arrays.copyOf(chordId, chordId.length);
    }

    public byte[] getSkipChordId() {
        return Arrays.copyOf(skipChordId, skipChordId.length);
    }
    
//    @Override
//    protected void innerValidate() {
//        Validate.notNull(chordId);
//        Validate.notNull(skipChordId);
//        Validate.isTrue(chordId.length > 0);
//        Validate.isTrue(skipChordId.length > 0);
//    }
}
