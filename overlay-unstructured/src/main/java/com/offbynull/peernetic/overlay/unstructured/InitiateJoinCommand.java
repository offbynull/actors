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

final class InitiateJoinCommand {
    private ByteBuffer secret;

    public InitiateJoinCommand(ByteBuffer secret) {
        Validate.notNull(secret);
        Validate.isTrue(secret.remaining() == Constants.SECRET_SIZE);

        this.secret = ByteBufferUtils.copyContents(secret).asReadOnlyBuffer();
    }

    public ByteBuffer getSecret() {
        return secret;
    }
}
