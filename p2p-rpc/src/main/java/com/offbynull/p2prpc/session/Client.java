package com.offbynull.p2prpc.session;

import java.io.IOException;

public interface Client<A> {
    byte[] send(A address, byte[] data, long timeout) throws IOException, InterruptedException;
}