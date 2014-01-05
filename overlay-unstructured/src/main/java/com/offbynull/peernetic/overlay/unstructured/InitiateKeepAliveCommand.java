/*
 * Copyright (c) 2013, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class InitiateKeepAliveCommand<A> {
    private A address;
    private CommandResponseListener<Boolean> callback;
    private ByteBuffer secret;

    public InitiateKeepAliveCommand(A address, ByteBuffer secret, CommandResponseListener<Boolean> callback) {
        Validate.notNull(address);
        Validate.notNull(secret);
        Validate.notNull(callback);
        Validate.isTrue(secret.remaining() == UnstructuredService.SECRET_SIZE);

        this.address = address;
        this.secret = ByteBufferUtils.copyContents(secret).asReadOnlyBuffer();
        this.callback = callback;
    }

    public A getAddress() {
        return address;
    }

    public ByteBuffer getSecret() {
        return secret;
    }

    public CommandResponseListener<Boolean> getCallback() {
        return callback;
    }
    
}
