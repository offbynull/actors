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
package com.offbynull.peernetic.actor.network.serializers.xstream;

import com.offbynull.peernetic.actor.network.Serializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * A serializer that uses XStream. Result can be deserialized with {@link XStreamBinaryDeserializer}.
 * @author Kasra Faghihi
 */
public final class XStreamSerializer implements Serializer {

    private XStream xstream;
    
    /**
     * Constructs a {@link XStreamBinarySerializer} using XStream's {@link BinaryStreamDriver}.
     */
    public XStreamSerializer() {
        this(new XStream(new BinaryStreamDriver()));
    }

    /**
     * Constructs a {@link XStreamBinarySerializer}.
     * @param xstream xstream object
     * @throws NullPointerException
     */
    public XStreamSerializer(XStream xstream) {
        Validate.notNull(xstream);
        this.xstream = xstream;
    }

    @Override
    public ByteBuffer serialize(Object content) {
        Validate.notNull(content);
        
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            xstream.toXML(content, os);
            return ByteBuffer.wrap(os.toByteArray());
        } catch (RuntimeException re) {
            throw new IllegalStateException(re);
        }
    }
}
