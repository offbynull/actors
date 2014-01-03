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
package com.offbynull.peernetic.rpc.transport.common;

import java.nio.ByteBuffer;
import org.apache.commons.lang3.Validate;

final class MessageMarker {
    /**
     * Number of bytes for a marker.
     */
    public static final int MARKER_SIZE = 1;
    
    private static final byte REQUEST_MARKER = 0;
    private static final byte RESPONSE_MARKER = 1;
    
    private MessageMarker() {
        // Do nothing
    }
    
    public static void writeMarker(ByteBuffer data, MessageType type) {
        Validate.notNull(data);
        Validate.notNull(type);
        Validate.isTrue(data.remaining() >= 1);
        
        switch (type) {
            case REQUEST:
                data.put(REQUEST_MARKER);
                break;
            case RESPONSE:
                data.put(RESPONSE_MARKER);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
    
    public static MessageType readMarker(ByteBuffer data) {
        Validate.notNull(data);
        Validate.isTrue(data.remaining() >= 1);
        
        switch (data.get()) {
            case REQUEST_MARKER:
                return MessageType.REQUEST;
            case RESPONSE_MARKER:
                return MessageType.RESPONSE;
            default:
                return null;
        }
    }
    
    public static void skipOver(ByteBuffer data) {
        Validate.notNull(data);
        Validate.isTrue(data.remaining() > 0);
        
        data.position(data.position() + 1);
    }
}
