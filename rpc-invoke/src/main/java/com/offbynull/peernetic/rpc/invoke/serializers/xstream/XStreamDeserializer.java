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
package com.offbynull.peernetic.rpc.invoke.serializers.xstream;

import com.offbynull.peernetic.rpc.invoke.Deserializer;
import com.offbynull.peernetic.rpc.invoke.Deserializer.DeserializerResult;
import com.offbynull.peernetic.rpc.invoke.InvokeData;
import com.offbynull.peernetic.rpc.invoke.SerializationType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import java.io.ByteArrayInputStream;
import org.apache.commons.lang3.Validate;

/**
 * A deserializer that uses XStream. Result must have been serialized with {@link XStreamBinarySerializer}.
 * @author Kasra Faghihi
 */
public final class XStreamDeserializer implements Deserializer {
    
    private XStream xstream;
    
    /**
     * Constructs a {@link XStreamBinaryDeserializer} using XStream's {@link BinaryStreamDriver}.
     */
    public XStreamDeserializer() {
        this(new XStream(new BinaryStreamDriver()));
    }

    /**
     * Constructs a {@link XStreamBinaryDeserializer}.
     * @param xstream xstream object
     * @throws NullPointerException
     */
    public XStreamDeserializer(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }

    @Override
    public DeserializerResult deserialize(byte[] data) {
        Validate.notNull(data);
        
        try {
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
        } catch (RuntimeException re) {
            throw new IllegalStateException(re);
        }
    }
}
