package com.offbynull.peernetic.common.message;

public abstract class Response extends Message {

    public Response() {
    }

    public Response(byte[] nonce) {
        super(nonce);
    }
    
    // CLEAN THIS UP LATER -- This is useless to have because it just calls super.hashCode();
    @Override
    public int hashCode() { // NOPMD
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return super.equals(obj);
    }
}
