package com.offbynull.peernetic;

import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class ByteArrayNonce implements Nonce<byte[]> {
    private byte[] value;

    public ByteArrayNonce(byte[] value) {
        Validate.isTrue(value.length > 0);
        
        this.value = Arrays.copyOf(value, value.length);
    }

    @Override
    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.value);
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
        final ByteArrayNonce other = (ByteArrayNonce) obj;
        if (!Arrays.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }
    
}
