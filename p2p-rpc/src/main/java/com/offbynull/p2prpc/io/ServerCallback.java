package com.offbynull.p2prpc.io;

public interface ServerCallback {
    byte[] incomingMessage(byte[] data);
}
