package com.offbynull.peernetic.common;

import java.util.Arrays;

public final class ByteArrayNonceWrapper implements NonceWrapper<byte[]> {

    @Override
    public Nonce<byte[]> wrap(byte[] value) {
        return new ByteArrayNonce(Arrays.copyOf(value, value.length));
    }
    
}
