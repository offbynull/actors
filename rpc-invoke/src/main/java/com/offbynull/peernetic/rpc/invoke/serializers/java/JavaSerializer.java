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
package com.offbynull.peernetic.rpc.invoke.serializers.java;

import com.offbynull.peernetic.rpc.invoke.InvokeData;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.apache.commons.lang3.Validate;

/**
 * A serializer that uses Java's internal {@link ObjectOutputStream}.
 *
 * @author Kasra Faghihi
 */
public final class JavaSerializer implements Serializer {

    @Override
    public byte[] serializeMethodCall(InvokeData invokeData) {
        Validate.notNull(invokeData);
        return serialize(SerializationType.METHOD_CALL, invokeData);
    }

    @Override
    public byte[] serializeMethodReturn(Object ret) {
        return serialize(SerializationType.METHOD_RETURN, ret);
    }

    @Override
    public byte[] serializeMethodThrow(Throwable err) {
        Validate.notNull(err);
        return serialize(SerializationType.METHOD_THROW, err);
    }

    private byte[] serialize(SerializationType type, Object obj) {
        Validate.notNull(type);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);) {
            oos.writeInt(type.ordinal());
            oos.writeObject(obj);
            return baos.toByteArray();
        } catch (RuntimeException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
