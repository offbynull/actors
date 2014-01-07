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
package com.offbynull.peernetic.rpc.transport.serializers.xstream;

import com.offbynull.peernetic.common.utils.ByteBufferUtils;
import com.offbynull.peernetic.rpc.transport.Deserializer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.binary.BinaryStreamDriver;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

/**
 * A deserializer that uses XStream. Result must have been serialized with {@link XStream}.
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
    public Object deserialize(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        Object obj;
        try {
            byte[] bufferData = ByteBufferUtils.copyContentsToArray(buffer);
            
            ByteArrayInputStream is = new ByteArrayInputStream(bufferData);
            obj = xstream.fromXML(is);
        } catch (RuntimeException re) {
            throw new IllegalStateException(re);
        }
        
        Validate.isTrue(obj != null);
        
        return obj;
    }
}
