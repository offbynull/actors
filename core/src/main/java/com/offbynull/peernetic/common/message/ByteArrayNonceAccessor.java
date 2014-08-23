package com.offbynull.peernetic.common.message;

public final class ByteArrayNonceAccessor implements NonceAccessor<byte[]> {

    @Override
    public Nonce<byte[]> get(Object object) {
        byte[] value = getValue(object);
        return new ByteArrayNonce(value);
    }
    
}
