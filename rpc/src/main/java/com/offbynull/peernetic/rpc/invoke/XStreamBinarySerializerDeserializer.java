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
package com.offbynull.peernetic.rpc.invoke;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.commons.lang3.Validate;

/**
 * A serializer/deserializer that uses XStream.
 * @author Kasra F
 */
public final class XStreamBinarySerializerDeserializer implements Serializer, Deserializer {
    
    private XStream xstream = new XStream(new BinaryStreamDriver());

    @Override
    public byte[] serializeMethodCall(InvokeData invokeData) {
        return serialize(SerializationType.METHOD_CALL, invokeData);
    }

    @Override
    public byte[] serializeMethodReturn(Object ret) {
        return serialize(SerializationType.METHOD_RETURN, ret);
    }

    @Override
    public byte[] serializeMethodThrow(Throwable err) {
        return serialize(SerializationType.METHOD_THROW, err);
    }
    
    private byte[] serialize(SerializationType type, Object obj) {
        Validate.notNull(type);
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(type.ordinal());
        xstream.toXML(obj, os);
        return os.toByteArray();
    }
    
    @Override
    public DeserializerResult deserialize(byte[] data) {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        int ordinal = is.read();
        SerializationType type = SerializationType.values()[ordinal];
        Object obj = xstream.fromXML(is);
        
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
    }
}
