package com.offbynull.peernetic.common.message;

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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Arrays.hashCode(this.nonce);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Message other = (Message) obj;
        if (!Arrays.equals(this.nonce, other.nonce)) {
            return false;
        }
        return true;
    }
    
    
}
