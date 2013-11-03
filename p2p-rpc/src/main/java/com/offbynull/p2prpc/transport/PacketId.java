package com.offbynull.p2prpc.transport;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class PacketId {
    private byte[] id;

    public PacketId(byte[] id) {
        if (id.length != 16) {
            throw new IllegalArgumentException();
        }
        
        this.id = Arrays.copyOf(id, id.length);
    }

    public byte[] prependId(byte[] buffer) {
        ByteBuffer ret = ByteBuffer.allocate(16 + buffer.length);
        ret.put(id);
        ret.put(buffer);
        
        return ret.array();
    }

    public static PacketId extractPrependedId(byte[] buffer) {
        return extractPrependedId(ByteBuffer.wrap(buffer));
    }

    public static PacketId extractPrependedId(ByteBuffer buffer) {
        byte[] extractedId = new byte[16];
        buffer.mark();
        buffer.get(extractedId);
        buffer.reset();
        
        return new PacketId(extractedId);
    }

    public static byte[] removePrependedId(byte[] buffer) {
        return removePrependedId(buffer);
    }
    
    public static byte[] removePrependedId(ByteBuffer buffer) {
        byte[] extractedData = new byte[buffer.limit() - 16];
        
        buffer.mark();
        buffer.position(16);
        buffer.get(extractedData);
        buffer.reset();
        
        return extractedData;
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
        final PacketId other = (PacketId) obj;
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
