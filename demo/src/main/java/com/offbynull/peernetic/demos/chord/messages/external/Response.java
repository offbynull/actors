package com.offbynull.peernetic.demos.chord.messages.external;

public abstract class Response extends Message {

    public Response(byte[] nonce) {
        super(nonce);
    }
    
}
