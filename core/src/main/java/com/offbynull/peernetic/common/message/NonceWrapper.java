package com.offbynull.peernetic.common.message;

public interface NonceWrapper<T> {
    
    Nonce<T> wrap(T value);
}
