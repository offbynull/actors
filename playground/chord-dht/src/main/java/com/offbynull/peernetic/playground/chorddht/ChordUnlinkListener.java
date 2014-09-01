package com.offbynull.peernetic.playground.chorddht;

public interface ChordUnlinkListener<A> {
    void unlinked(A id, A dst);
}
