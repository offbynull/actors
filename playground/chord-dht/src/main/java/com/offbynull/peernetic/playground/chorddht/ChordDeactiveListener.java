package com.offbynull.peernetic.playground.chorddht;

public interface ChordDeactiveListener<A> {
    void deactive(A id, Type type);
    
    enum Type {
        GRACEFUL,
        ERROR
    };
}
