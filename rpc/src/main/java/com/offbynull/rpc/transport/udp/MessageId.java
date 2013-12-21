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
package com.offbynull.rpc.transport.udp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class MessageId {
    private byte[] id;

    public MessageId(byte[] id) {
        Validate.isTrue(id.length == 16);
        
        this.id = Arrays.copyOf(id, id.length);
    }

    public byte[] prependId(byte[] buffer) {
        Validate.notNull(buffer);
        
        ByteBuffer ret = ByteBuffer.allocate(16 + buffer.length);
        ret.put(id);
        ret.put(buffer);
        
        return ret.array();
    }
    
    public void writeId(ByteBuffer buffer) {
        Validate.notNull(buffer);
        Validate.isTrue(buffer.remaining() >= 16);
        
        buffer.put(id);
    }

    public static MessageId extractPrependedId(byte[] buffer) {
        Validate.notNull(buffer);
        
        return extractPrependedId(ByteBuffer.wrap(buffer));
    }

    public static MessageId extractPrependedId(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        byte[] extractedId = new byte[16];
        buffer.mark();
        buffer.get(extractedId);
        buffer.reset();
        
        return new MessageId(extractedId);
    }

    public static byte[] removePrependedId(byte[] buffer) {
        Validate.notNull(buffer);
        
        return removePrependedId(ByteBuffer.wrap(buffer));
    }
    
    public static byte[] removePrependedId(ByteBuffer buffer) {
        Validate.notNull(buffer);
        
        byte[] extractedData = new byte[buffer.remaining() - 16];
        
        buffer.mark();
        buffer.position(buffer.position() + 16);
        buffer.get(extractedData);
        buffer.reset();
        
        return extractedData;
    }

    public static void skipOver(ByteBuffer data) {
        Validate.notNull(data);
        Validate.isTrue(data.remaining() > 0);
        
        data.position(data.position() + 16);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MessageId other = (MessageId) obj;
        if (!Arrays.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PacketId{" + "id=" + Arrays.toString(id) + '}';
    }
    
}
