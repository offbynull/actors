package com.offbynull.peernetic.common;

public abstract class Request extends Message {

    public Request() {
    }

    public Request(byte[] nonce) {
        super(nonce);
    }
    
}