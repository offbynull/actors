package com.offbynull.peernetic.common;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public abstract class Message {
    private static final byte[] EMPTY_NONCE = new byte[0];
    
    @NonceField
    private byte[] nonce;

    public Message(byte[] nonce) {
        Validate.isTrue(nonce.length > 0);
        this.nonce = Arrays.copyOf(nonce, nonce.length);
    }

    public Message() {
        this.nonce = EMPTY_NONCE;
    }

    public final byte[] getNonce() {
        return Arrays.copyOf(nonce, nonce.length);
    }
    
    @ValidationMethod
    public final void validate() {
        Validate.notNull(nonce);
        innerValidate();
    }
    
    protected abstract void innerValidate();
}
