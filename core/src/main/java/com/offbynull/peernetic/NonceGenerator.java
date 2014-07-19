package com.offbynull.peernetic;

public interface NonceGenerator<T> {
    
    Nonce<T> generate();
}
