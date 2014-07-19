package com.offbynull.peernetic;

public interface NonceGenerator<T> {
    
    public Nonce<T> generate();
}
