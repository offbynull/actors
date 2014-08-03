package com.offbynull.peernetic.demos.unstructured.messages.external;

public abstract class Request extends Message {

    public Request(byte[] nonce) {
        super(nonce);
    }
    
}
