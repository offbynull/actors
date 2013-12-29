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

import com.offbynull.peernetic.rpc.invoke.Deserializer;
import com.offbynull.peernetic.rpc.invoke.InvokeData;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * A deserializer that uses Java's internal {@link ObjectInputStream}.
 *
 * @author Kasra Faghihi
 */
public final class JavaDeserializer implements Deserializer {


    @Override
    public DeserializerResult deserialize(byte[] data) {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);) {
            int ordinal = ois.readInt();
            SerializationType type = SerializationType.values()[ordinal];
            Object obj = ois.readObject();

            switch (type) {
                case METHOD_CALL:
                    if (!(obj instanceof InvokeData)) {
                        throw new IllegalArgumentException("Inconsistent type");
                    }
                    break;
                case METHOD_RETURN:
                    break;
                case METHOD_THROW:
                    if (!(obj instanceof Throwable)) {
                        throw new IllegalArgumentException("Inconsistent type");
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            return new DeserializerResult(type, obj);
        } catch (RuntimeException | IOException | ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
