package com.offbynull.peernetic.overlay.unstructured;

interface KeepAliveListener<A> {
    void keepAliveSuccessful(A address);
    void keepAliveFailed(A address, FailReason reason);
    
    public enum FailReason {
        NO_RESPONSE,
        REJECTED_RESPONSE
    }
}
