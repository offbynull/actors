package com.offbynull.peernetic.playground.chorddht;

public interface ChordLinkListener<A> {
    void linked(A id, A dst);
}
