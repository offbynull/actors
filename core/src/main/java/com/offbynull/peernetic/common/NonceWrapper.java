package com.offbynull.peernetic.common;

public interface NonceWrapper<T> {
    
    Nonce<T> wrap(T value);
}
