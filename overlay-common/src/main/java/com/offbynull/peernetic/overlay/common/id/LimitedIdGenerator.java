package com.offbynull.peernetic.overlay.common.id;

public interface LimitedIdGenerator {
    LimitedId generate(byte[] limit);
}
