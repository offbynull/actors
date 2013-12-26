package com.offbynull.peernetic.overlay.unstructured;

public interface UnstructuredService<A> {
    int SERVICE_ID = 5000;
    int SECRET_SIZE = 16;

    State<A> getState();

    boolean join(byte[] secret);
    boolean keepAlive(byte[] secret);
}
