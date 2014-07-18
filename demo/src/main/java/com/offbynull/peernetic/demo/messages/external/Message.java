package com.offbynull.peernetic.demo.messages.external;

import org.apache.commons.lang3.Validate;

public abstract class Message {
    private String nonce;

    public Message(String nonce) {
        Validate.isTrue(nonce.length() == 8);
        this.nonce = nonce;
    }

    public final String getNonce() {
        return nonce;
    }
    
    public final void validate() {
        Validate.isTrue(nonce.length() == 8);
        innerValidate();
    }
    
    protected void innerValidate() {
    }
}
