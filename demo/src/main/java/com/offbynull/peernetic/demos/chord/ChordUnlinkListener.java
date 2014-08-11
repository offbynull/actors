package com.offbynull.peernetic.demos.chord;

public interface ChordUnlinkListener<A> {
    void unlinked(A id, A dst);
}
