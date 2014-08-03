package com.offbynull.peernetic.demos.chord.messages.external;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public abstract class Message {
    public static final int NONCE_LENGTH = 8;
    private byte[] nonce;

    public Message(byte[] nonce) {
        Validate.isTrue(nonce.length == NONCE_LENGTH);
        this.nonce = Arrays.copyOf(nonce, nonce.length);
    }

    public final byte[] getNonce() {
        return Arrays.copyOf(nonce, nonce.length);
    }
    
    public final void validate() {
        Validate.validState(nonce.length == NONCE_LENGTH);
        innerValidate();
    }
    
    protected abstract void innerValidate();
}
