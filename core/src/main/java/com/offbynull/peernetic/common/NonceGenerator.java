package com.offbynull.peernetic.common;

public interface NonceGenerator<T> {
    
    Nonce<T> generate();
}
