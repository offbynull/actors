package com.offbynull.rpc.invoke;

import java.io.IOException;

/**
 * Manipulates serialized data.
 * @author Kasra F
 */
public interface Filter {
    /**
     * Modifies serialized data.
     * @param data data to be modified
     * @return modified data
     * @throws IOException on error
     * @throws NullPointerException if any arguments are {@code null}
     */
    byte[] modify(byte[] data) throws IOException;
    /**
     * Unmodifies serialized data.
     * @param data data to be unmodified
     * @return unmodified data
     * @throws IOException on error
     * @throws NullPointerException if any arguments are {@code null}
     */
    byte[] unmodify(byte[] data) throws IOException;
}
