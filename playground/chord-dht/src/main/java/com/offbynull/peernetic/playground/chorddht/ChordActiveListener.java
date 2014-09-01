package com.offbynull.peernetic.playground.chorddht;

public interface ChordActiveListener<A> {
    void active(A id, Mode mode);
    
    enum Mode {
        SEED,
        JOIN
    }
}
