package com.offbynull.peernetic.common;

public abstract class Response extends Message {

    public Response(byte[] nonce) {
        super(nonce);
    }
    
}
