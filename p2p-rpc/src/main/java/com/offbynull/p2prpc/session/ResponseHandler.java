package com.offbynull.p2prpc.session;

public interface ResponseHandler {
    void responseReady(byte[] data);
    void terminate();
}
