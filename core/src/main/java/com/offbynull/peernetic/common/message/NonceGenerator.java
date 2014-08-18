package com.offbynull.peernetic.common.message;

public interface NonceGenerator<T> {

    Nonce<T> generate();
}
