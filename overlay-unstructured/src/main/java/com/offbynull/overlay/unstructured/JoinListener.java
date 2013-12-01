package com.offbynull.overlay.unstructured;

import java.nio.ByteBuffer;

interface JoinListener<A> {
    void joinComplete(A address, ByteBuffer secret);
    void joinFailed(A address, FailReason reason);
    
    public enum FailReason {
        NO_RESPONSE,
        REJECTED_RESPONSE
    }
}
