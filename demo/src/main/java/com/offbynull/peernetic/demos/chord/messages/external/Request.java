package com.offbynull.peernetic.demos.chord.messages.external;

public abstract class Request extends Message {

    public Request(byte[] nonce) {
        super(nonce);
    }
    
}
