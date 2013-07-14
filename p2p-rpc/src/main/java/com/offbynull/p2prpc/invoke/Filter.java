package com.offbynull.p2prpc.invoke;

import java.io.IOException;

public interface Filter {
    byte[] modify(byte[] data) throws IOException;
    byte[] unmodify(byte[] data) throws IOException;
}
