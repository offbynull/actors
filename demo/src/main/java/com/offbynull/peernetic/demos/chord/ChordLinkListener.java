package com.offbynull.peernetic.demos.chord;

public interface ChordLinkListener<A> {
    void linked(A id, A dst);
}
