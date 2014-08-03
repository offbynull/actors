package com.offbynull.peernetic.demos.unstructured.messages.external;

public abstract class Response extends Message {

    public Response(byte[] nonce) {
        super(nonce);
    }
    
}
