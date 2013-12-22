package com.offbynull.peernetic.overlay.common;

public interface LimitedIdGenerator {
    LimitedId generate(byte[] limit);
}
