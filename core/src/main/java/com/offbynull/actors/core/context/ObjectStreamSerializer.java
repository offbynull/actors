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
package com.offbynull.actors.core.context;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.Validate;

/**
 * A {@link Serializer} that uses {@link ObjectOutputStream} and {@link ObjectInputStream} to perform serialization operations.
 * @author Kasra Faghihi
 */
public final class ObjectStreamSerializer implements Serializer {

    @Override
    public byte[] serialize(SourceContext ctx) {
        Validate.notNull(ctx);
        
        byte[] ret;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(ctx);
            oos.flush();
            ret = baos.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
        
        return ret;
    }

    @Override
    public SourceContext unserialize(byte[] data) {
        Validate.notNull(data);
        
        SourceContext ctx;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data); ObjectInputStream ois = new ObjectInputStream(bais)) {
            ctx = (SourceContext) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
        
        return ctx;
    }
    
}
