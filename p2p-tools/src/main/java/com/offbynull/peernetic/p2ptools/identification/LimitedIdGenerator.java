package com.offbynull.peernetic.p2ptools.identification;

public interface LimitedIdGenerator {
    LimitedId generate(byte[] limit);
}
