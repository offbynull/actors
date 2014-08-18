package com.offbynull.peernetic.common.message;

public abstract class Response extends Message {

    public Response() {
    }

    public Response(byte[] nonce) {
        super(nonce);
    }
    
}
