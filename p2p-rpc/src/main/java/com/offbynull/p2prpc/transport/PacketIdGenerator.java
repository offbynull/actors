package com.offbynull.p2prpc.transport;

/**
 * Generates IDs for UDP packets, such that a response can be properly paired with a request.
 */
public interface PacketIdGenerator {
    byte[] generate();
}
