package com.offbynull.peernetic.common;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public abstract class Message {
    @NonceField
    private byte[] nonce;

    public Message(byte[] nonce) {
        Validate.isTrue(nonce.length > 0);
        this.nonce = Arrays.copyOf(nonce, nonce.length);
    }

    public final byte[] getNonce() {
        return Arrays.copyOf(nonce, nonce.length);
    }
    
    @ValidationMethod
    public final void validate() {
        Validate.validState(nonce.length > 0);
        innerValidate();
    }
    
    protected abstract void innerValidate();
}
