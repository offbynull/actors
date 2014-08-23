package com.offbynull.peernetic.demos.chord;

public interface ChordActiveListener<A> {
    void active(A id, Mode mode);
    
    enum Mode {
        SEED,
        JOIN
    }
}
