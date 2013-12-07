package com.offbynull.rpccommon.filters.selfblock;

import java.net.SocketException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class SelfBlockId {
    public static final int LENGTH = 16;

    private ByteBuffer id;

    public SelfBlockId() {
        SecureRandom random = new SecureRandom();
        byte[] data = new byte[LENGTH];
        random.nextBytes(data);
        id = ByteBuffer.wrap(data);
    }

    public SelfBlockId(ByteBuffer id) {
        Validate.notNull(id);
        Validate.isTrue(id.remaining() == LENGTH);
        
        this.id = ByteBuffer.allocate(id.remaining());
        this.id.put(id);
    }

    public ByteBuffer getBuffer() {
        return id.asReadOnlyBuffer();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.id);
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
        final SelfBlockId other = (SelfBlockId) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
