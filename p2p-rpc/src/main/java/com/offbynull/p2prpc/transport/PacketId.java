package com.offbynull.p2prpc.transport;

import java.util.Arrays;

public final class PacketId {
    private byte[] id;

    public PacketId(byte[] id) {
        if (id.length != 16) {
            throw new IllegalArgumentException();
        }
        
        this.id = Arrays.copyOf(id, id.length);
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
