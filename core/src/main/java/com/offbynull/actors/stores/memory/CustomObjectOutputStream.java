/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.stores.memory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

final class CustomObjectOutputStream extends ObjectOutputStream {

    private final ByteArrayOutputStream cbaos;

    CustomObjectOutputStream() throws IOException {
        this(new ByteArrayOutputStream());
    }

    private CustomObjectOutputStream(ByteArrayOutputStream cbaos) throws IOException {
        super(cbaos);
        this.cbaos = cbaos;
    }

    public byte[] toByteArray() throws IOException {
        flush();
        cbaos.flush(); // justincase
        return cbaos.toByteArray();
    }
}
