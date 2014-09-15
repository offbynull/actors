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

import com.offbynull.peernetic.common.message.Request;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class GetClosestFingerRequest extends Request {
    private byte[] id;
    private byte[] skipId; //get closest that isn't skipId

    public GetClosestFingerRequest(byte[] id, byte[] skipId) {
        this.id = Arrays.copyOf(id, id.length);
        this.skipId = Arrays.copyOf(skipId, skipId.length);
        validate();
    }

    public byte[] getId() {
        return Arrays.copyOf(id, id.length);
    }

    public byte[] getSkipId() {
        return Arrays.copyOf(skipId, skipId.length);
    }
    
    @Override
    protected void innerValidate() {
        Validate.notNull(id);
        Validate.notNull(skipId);
        Validate.isTrue(id.length > 0);
        Validate.isTrue(skipId.length > 0);
    }
}
