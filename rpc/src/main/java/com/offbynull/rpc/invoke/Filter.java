package com.offbynull.rpc.invoke;

import java.io.IOException;

public interface Filter {
    byte[] modify(byte[] data) throws IOException;
    byte[] unmodify(byte[] data) throws IOException;
}
