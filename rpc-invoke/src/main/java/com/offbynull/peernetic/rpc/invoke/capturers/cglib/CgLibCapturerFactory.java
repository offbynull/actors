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
package com.offbynull.peernetic.rpc.invoke.capturers.cglib;

import com.offbynull.peernetic.rpc.invoke.Capturer;
import com.offbynull.peernetic.rpc.invoke.CapturerFactory;
import com.offbynull.peernetic.rpc.invoke.Deserializer;
import com.offbynull.peernetic.rpc.invoke.Serializer;
import com.offbynull.peernetic.rpc.invoke.serializers.java.JavaDeserializer;
import com.offbynull.peernetic.rpc.invoke.serializers.java.JavaSerializer;
import org.apache.commons.lang3.Validate;

public final class CgLibCapturerFactory implements CapturerFactory {

    private Serializer serializer = new JavaSerializer();
    private Deserializer deserializer = new JavaDeserializer();
    
    public Serializer getSerializer() {
        return serializer;
    }

    public void setSerializer(Serializer serializer) {
        Validate.notNull(serializer);
        this.serializer = serializer;
    }

    public Deserializer getDeserializer() {
        return deserializer;
    }

    public void setDeserializer(Deserializer deserializer) {
        Validate.notNull(deserializer);
        this.deserializer = deserializer;
    }


    @Override
    public <T> Capturer<T> createCapturer(Class<T> type) {
        Validate.notNull(type);
        
        CglibCapturer<T> cglibCapturer = new CglibCapturer(type, serializer, deserializer);
        return cglibCapturer;
    }
    
}
