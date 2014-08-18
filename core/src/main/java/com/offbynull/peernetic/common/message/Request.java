package com.offbynull.peernetic.common.message;

public abstract class Request extends Message {

    public Request() {
    }

    public Request(byte[] nonce) {
        super(nonce);
    }
    
}
